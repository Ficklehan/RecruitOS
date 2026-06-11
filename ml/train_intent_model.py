#!/usr/bin/env python3
"""
RecruitOS 候选人意向预测模型训练脚本

训练目标：预测候选人是否会接受 Offer（二分类）
模型：LightGBM
特征：15+ 维通信/行为/简历/薪资特征
输出：PMML 模型文件 + SHAP 解释器
"""
import argparse
import json
import os
import pickle
from datetime import datetime

import numpy as np
import pandas as pd
import lightgbm as lgb
import shap
try:
    from sklearn2pmml import sklearn2pmml, PMMLPipeline
    _HAS_PMML = True
except ImportError:
    _HAS_PMML = False
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import (
    roc_auc_score, precision_score, recall_score, f1_score,
    classification_report, confusion_matrix
)

# ============================================================
# 特征工程
# ============================================================

FEATURE_COLUMNS = [
    # 通信特征
    'reply_speed_avg',          # 平均回复间隔（小时，归一化）
    'reply_speed_trend',        # 回复速度趋势（-1到1，正=加速）
    'message_length_avg',       # 候选人消息平均长度
    'question_count',           # 主动提问数量
    'question_quality_score',   # 提问质量分（0-1）
    # 行为特征
    'interview_attendance',     # 面试出席率（0-1）
    'interview_reschedule_cnt', # 改期次数
    'interview_late_minutes',   # 迟到分钟数
    # 简历特征
    'past_tenure_avg_months',   # 历史平均在职时长
    'job_hop_count_3y',         # 近3年跳槽次数
    'current_company_tier',     # 当前公司级别（3=BAT, 2=TMD, 1=其他）
    # 匹配特征
    'match_score',              # 岗位匹配分
    'match_score_pct',          # 匹配分在同岗位百分位
    # 薪资特征
    'salary_gap_pct',           # 期望vs预算差距百分比
    'salary_percentile',        # 期望在同级别分位数
    # 进度特征
    'pipeline_stage_days',      # 已用天数
    'stages_completed',         # 已完成阶段数
    # 竞品特征
    'competing_offers',         # 是否有竞品Offer（0/1）
    'market_hotness',           # 岗位市场热度
]

CATEGORICAL_FEATURES = ['current_company_tier']


def load_training_data(data_path: str) -> pd.DataFrame:
    """从 JSON 或 CSV 加载训练数据"""
    if data_path.endswith('.csv'):
        return pd.read_csv(data_path)
    elif data_path.endswith('.json'):
        return pd.read_json(data_path)
    elif data_path.endswith('.jsonl'):
        return pd.read_json(data_path, lines=True)
    else:
        raise ValueError(f"Unsupported format: {data_path}")


def generate_synthetic_data(n_samples: int = 500) -> pd.DataFrame:
    """生成模拟训练数据（用于开发测试）"""
    np.random.seed(42)
    data = {
        'reply_speed_avg': np.clip(np.random.normal(12, 8, n_samples), 1, 72),
        'reply_speed_trend': np.clip(np.random.normal(0, 0.3, n_samples), -1, 1),
        'message_length_avg': np.random.poisson(80, n_samples),
        'question_count': np.random.poisson(3, n_samples),
        'question_quality_score': np.clip(np.random.beta(2, 3, n_samples), 0, 1),
        'interview_attendance': np.clip(np.random.beta(8, 1, n_samples), 0, 1),
        'interview_reschedule_cnt': np.random.poisson(0.5, n_samples),
        'interview_late_minutes': np.random.exponential(5, n_samples),
        'past_tenure_avg_months': np.clip(np.random.normal(24, 12, n_samples), 3, 60),
        'job_hop_count_3y': np.random.poisson(1.5, n_samples),
        'current_company_tier': np.random.choice([1, 2, 3], n_samples, p=[0.5, 0.3, 0.2]),
        'match_score': np.clip(np.random.normal(72, 15, n_samples), 0, 100),
        'match_score_pct': np.clip(np.random.beta(3, 2, n_samples), 0, 1),
        'salary_gap_pct': np.clip(np.random.normal(5, 15, n_samples), -30, 50),
        'salary_percentile': np.clip(np.random.beta(3, 3, n_samples), 0, 1),
        'pipeline_stage_days': np.random.poisson(21, n_samples),
        'stages_completed': np.random.choice([1, 2, 3, 4], n_samples),
        'competing_offers': np.random.binomial(1, 0.15, n_samples),
        'market_hotness': np.clip(np.random.beta(2, 3, n_samples), 0, 1),
    }
    df = pd.DataFrame(data)

    # 生成 label：Offer 接受 = 1
    # 基于多个特征加权决定
    prob = (
        0.50
        + 0.08 * (1 - df['reply_speed_avg'] / 72)      # 回复快 → 高意向
        + 0.10 * df['interview_attendance']              # 出勤高 → 高意向
        + 0.08 * (df['match_score'] / 100)               # 匹配高 → 高意向
        - 0.12 * (df['salary_gap_pct'] / 50)             # 薪资差距大 → 低意向
        - 0.06 * (df['job_hop_count_3y'] / 5)            # 跳槽多 → 低意向
        - 0.10 * df['competing_offers']                  # 竞品Offer → 低意向
        + 0.04 * (df['current_company_tier'] / 3)        # 好公司 → 更高要求
        + 0.05 * (1 - df['interview_late_minutes'] / 30) # 不迟到 → 高意向
    )
    prob = np.clip(prob, 0.05, 0.95)
    df['label'] = np.random.binomial(1, prob)
    return df


def train_model(df: pd.DataFrame, output_dir: str):
    """训练 LightGBM 模型"""
    X = df[FEATURE_COLUMNS].copy()
    y = df['label'].values

    # 划分训练/测试集
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    # 训练
    model = lgb.LGBMClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.05,
        num_leaves=31,
        min_child_samples=20,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42,
        verbose=-1,
    )
    model.fit(
        X_train, y_train,
        categorical_feature=CATEGORICAL_FEATURES,
        eval_set=[(X_test, y_test)],
        eval_metric='auc',
    )

    # 评估
    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]
    auc = roc_auc_score(y_test, y_prob)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)

    print(f"\n=== 模型评估 ===")
    print(f"AUC:      {auc:.4f}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall:    {recall:.4f}")
    print(f"F1:        {f1:.4f}")
    print(f"\n分类报告:\n{classification_report(y_test, y_pred)}")

    # SHAP 可解释性
    explainer = shap.TreeExplainer(model)
    shap_values = explainer.shap_values(X_test[:100])

    # 计算全局特征重要性
    feature_importance = pd.DataFrame({
        'feature': FEATURE_COLUMNS,
        'importance': model.feature_importances_,
    }).sort_values('importance', ascending=False)

    print(f"\n=== Top 10 特征重要性 ===")
    print(feature_importance.head(10).to_string(index=False))

    # 保存模型
    os.makedirs(output_dir, exist_ok=True)
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

    # 保存为 joblib (Python)
    model_path = os.path.join(output_dir, f'intent_model_{timestamp}.joblib')
    joblib.dump(model, model_path)
    print(f"\n模型已保存: {model_path}")

    # 导出 PMML（Java 加载用）
    pmml_path = os.path.join(output_dir, f'intent_model_{timestamp}.pmml')
    if _HAS_PMML:
        try:
            pmml_pipeline = PMMLPipeline([("classifier", model)])
            sklearn2pmml(pmml_pipeline, pmml_path)
            print(f"PMML已导出: {pmml_path}")
        except Exception as e:
            print(f"PMML导出失败: {e}，跳过")
    else:
        print("sklearn2pmml未安装，跳过PMML导出。安装: pip install sklearn2pmml")

    # 保存特征重要性 JSON
    importance_path = os.path.join(output_dir, f'feature_importance_{timestamp}.json')
    feature_importance.to_json(importance_path, orient='records')
    print(f"特征重要性已保存: {importance_path}")

    # 保存 SHAP 解释器
    shap_path = os.path.join(output_dir, f'shap_explainer_{timestamp}.pkl')
    with open(shap_path, 'wb') as f:
        pickle.dump(explainer, f)
    print(f"SHAP解释器已保存: {shap_path}")

    # 保存模型元数据
    meta = {
        'model_version': timestamp,
        'auc': float(auc),
        'precision': float(precision),
        'recall': float(recall),
        'f1': float(f1),
        'features': FEATURE_COLUMNS,
        'pmml_path': pmml_path if _HAS_PMML else None,
        'categorical_features': CATEGORICAL_FEATURES,
        'num_features': len(FEATURE_COLUMNS),
        'num_samples': len(df),
        'num_positive': int(y.sum()),
        'num_negative': int(len(y) - y.sum()),
    }
    meta_path = os.path.join(output_dir, f'model_meta_{timestamp}.json')
    with open(meta_path, 'w') as f:
        json.dump(meta, f, indent=2, ensure_ascii=False)
    print(f"模型元数据已保存: {meta_path}")

    return model, explainer, meta


def main():
    parser = argparse.ArgumentParser(description='训练候选人意向预测模型')
    parser.add_argument('--data', help='训练数据路径 (CSV/JSON/JSONL)')
    parser.add_argument('--synthetic', type=int, default=500, help='生成模拟数据样本数')
    parser.add_argument('--output', default='./models', help='模型输出目录')
    args = parser.parse_args()

    if args.data:
        print(f"加载数据: {args.data}")
        df = load_training_data(args.data)
    else:
        print(f"生成 {args.synthetic} 条模拟训练数据")
        df = generate_synthetic_data(args.synthetic)

    print(f"训练数据: {len(df)} 条, 正样本: {df['label'].sum()}, 负样本: {(1-df['label']).sum()}")
    train_model(df, args.output)


if __name__ == '__main__':
    import joblib
    main()
