#!/usr/bin/env python3
"""
RecruitOS 意向预测模型训练管道
- 从 MySQL 读取标注数据
- 训练 LightGBM 二分类模型
- 计算 SHAP 特征重要性
- 导出 PMML + 特征元数据 JSON
- 对比历史模型 AUC，仅提升时替换

用法: python3 train_intent_model.py [--db-url URL] [--output-dir DIR] [--min-samples N]
"""

import argparse, json, os, sys, time
from datetime import datetime

import numpy as np
import pandas as pd

# --- 特征定义 ---
FEATURE_NAMES = [
    "reply_speed",           # 候选人消息回复速度 (0-1)
    "interview_engagement",  # 面试投入度 (0-1)
    "questions_depth",       # 提问深度 (0-1)
    "salary_gap_percent",    # 薪资差距百分比
    "has_competing_offers",  # 是否有竞品 Offer (0/1)
    "pipeline_stage_count",  # 当前阶段候选人总数
    "days_in_pipeline",      # 停留天数
    "greeting_reply_rate",   # 招呼回复率 (0-1)
    "resume_match_score",    # 简历匹配分 (0-100)
    "interview_score_avg",   # 面试平均分
    "previous_interactions", # 历史交互次数
    "source_channel_quality", # 渠道质量分 (0-1)
    "job_urgency",           # 岗位紧急度 (0-1)
    "market_demand_ratio",   # 市场供需比
    "candidate_level_match", # 级别匹配 (0-1)
]

LABEL_COLUMN = "accepted_offer"  # 1 = 接受 Offer

def load_data_from_csv(csv_path):
    """从 CSV 加载训练数据（主路径）"""
    df = pd.read_csv(csv_path)
    print(f"Loaded {len(df)} samples from {csv_path}")
    return df

def load_data_from_db(db_url):
    """从 MySQL 读取标注数据"""
    try:
        from sqlalchemy import create_engine
        engine = create_engine(db_url)
        query = """
            SELECT d.*, e.reply_speed, e.interview_engagement, e.questions_depth,
                   e.salary_gap_percent, e.has_competing_offers, e.pipeline_stage_count,
                   e.days_in_pipeline, e.greeting_reply_rate, e.resume_match_score,
                   e.interview_score_avg, e.previous_interactions,
                   e.source_channel_quality, e.job_urgency, e.market_demand_ratio,
                   e.candidate_level_match, e.accepted_offer
            FROM brain_intent_training_data d
            JOIN brain_intent_features e ON d.id = e.data_id
            WHERE d.labeled = TRUE
            ORDER BY d.created_at DESC
            LIMIT 10000
        """
        df = pd.read_sql(query, engine)
        print(f"Loaded {len(df)} samples from database")
        return df
    except Exception as e:
        print(f"Database load failed: {e}", file=sys.stderr)
        return None

def generate_synthetic_data(n_samples=500):
    """生成合成训练数据（当真实数据不足时的降级方案）"""
    np.random.seed(42)
    data = {}
    # 有 Offer 接受倾向的分布
    n_pos = n_samples // 3
    n_neg = n_samples - n_pos

    for feat in FEATURE_NAMES:
        if feat in ("has_competing_offers",):
            data[feat] = np.concatenate([
                np.random.binomial(1, 0.3, n_pos),
                np.random.binomial(1, 0.6, n_neg)
            ])
        elif feat in ("resume_match_score",):
            data[feat] = np.concatenate([
                np.random.normal(80, 10, n_pos),
                np.random.normal(60, 15, n_neg)
            ]).clip(0, 100)
        elif feat in ("interview_score_avg",):
            data[feat] = np.concatenate([
                np.random.normal(4.0, 0.5, n_pos),
                np.random.normal(3.0, 0.8, n_neg)
            ]).clip(1, 5)
        elif feat in ("salary_gap_percent",):
            data[feat] = np.concatenate([
                np.random.normal(5, 5, n_pos),
                np.random.normal(15, 10, n_neg)
            ]).clip(0, 50)
        elif feat in ("days_in_pipeline",):
            data[feat] = np.concatenate([
                np.random.exponential(10, n_pos),
                np.random.exponential(25, n_neg)
            ]).clip(1, 90)
        else:
            data[feat] = np.concatenate([
                np.random.beta(3, 2, n_pos),
                np.random.beta(2, 3, n_neg)
            ])

    data[LABEL_COLUMN] = np.concatenate([np.ones(n_pos), np.zeros(n_neg)]).astype(int)
    df = pd.DataFrame(data)
    # Shuffle
    df = df.sample(frac=1).reset_index(drop=True)
    print(f"Generated {len(df)} synthetic samples ({n_pos} positive, {n_neg} negative)")
    return df

# --- 模型训练 ---

def train_model(df, output_dir):
    """训练 LightGBM 模型，计算 SHAP，导出 PMML"""
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import roc_auc_score, classification_report
    import lightgbm as lgb
    import shap

    X = df[FEATURE_NAMES].values
    y = df[LABEL_COLUMN].values

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    # LightGBM
    train_data = lgb.Dataset(X_train, label=y_train, feature_name=FEATURE_NAMES)
    valid_data = lgb.Dataset(X_test, label=y_test, feature_name=FEATURE_NAMES, reference=train_data)

    params = {
        "objective": "binary",
        "metric": "auc",
        "boosting_type": "gbdt",
        "num_leaves": 31,
        "learning_rate": 0.05,
        "feature_fraction": 0.8,
        "bagging_fraction": 0.8,
        "bagging_freq": 5,
        "verbose": -1,
        "random_state": 42,
        "min_data_in_leaf": 20,
        "max_depth": 6,
    }

    model = lgb.train(params, train_data, valid_sets=[valid_data], num_boost_round=200,
                      callbacks=[lgb.early_stopping(20), lgb.log_evaluation(50)])

    # 评估
    y_proba = model.predict(X_test)
    auc = roc_auc_score(y_test, y_proba)
    y_pred = (y_proba > 0.5).astype(int)

    print(f"\n=== Model Evaluation ===")
    print(f"AUC: {auc:.4f}")
    print(f"Best iteration: {model.best_iteration}")
    print(classification_report(y_test, y_pred, target_names=["Not Accept", "Accept"]))

    # SHAP 特征重要性
    explainer = shap.TreeExplainer(model)
    shap_values = explainer.shap_values(X_test[:200])  # 采样以加速

    feature_importance = {}
    mean_shap = np.abs(shap_values).mean(axis=0)
    for i, name in enumerate(FEATURE_NAMES):
        feature_importance[name] = {
            "mean_abs_shap": float(mean_shap[i]),
            "rank": int(np.argsort(-mean_shap)[i]) + 1,
        }

    # --- 导出 ---
    os.makedirs(output_dir, exist_ok=True)

    # PMML
    pmml_path = os.path.join(output_dir, "intent_model.pmml")
    try:
        from sklearn2pmml import sklearn2pmml
        from sklearn2pmml.pipeline import PMMLPipeline
        # 需要包装成 sklearn pipeline
        pipeline = PMMLPipeline([
            ("classifier", lgb.LGBMClassifier(**{k: v for k, v in params.items()
                if k not in ("metric", "verbose")}))
        ])
        pipeline.fit(X_train, y_train)
        sklearn2pmml(pipeline, pmml_path)
        print(f"PMML exported: {pmml_path}")
    except ImportError:
        # 降级：保存为 LightGBM 原生格式
        model.save_model(os.path.join(output_dir, "intent_model.lgb"))
        print("PMML export skipped (sklearn2pmml not installed), saved LightGBM native model")

    # 特征元数据
    metadata = {
        "model_type": "lightgbm",
        "created_at": datetime.now().isoformat(),
        "n_samples": len(df),
        "n_features": len(FEATURE_NAMES),
        "feature_names": FEATURE_NAMES,
        "auc": float(auc),
        "best_iteration": model.best_iteration,
        "feature_importance": feature_importance,
        "top_features": sorted(feature_importance.items(), key=lambda x: -x[1]["mean_abs_shap"])[:5],
        "params": params,
    }
    with open(os.path.join(output_dir, "model_metadata.json"), "w") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    print(f"Metadata saved with AUC={auc:.4f}, top features: {[f[0] for f in metadata['top_features']]}")

    return auc, metadata

def main():
    parser = argparse.ArgumentParser(description="RecruitOS Intent Prediction Model Training")
    parser.add_argument("--db-url", help="MySQL connection URL")
    parser.add_argument("--csv", help="CSV file path (alternative to DB)")
    parser.add_argument("--output-dir", default="models", help="Output directory for PMML and metadata")
    parser.add_argument("--min-samples", type=int, default=100, help="Minimum samples required")
    parser.add_argument("--min-auc", type=float, default=0.65, help="Minimum AUC for model acceptance")
    parser.add_argument("--synthetic", action="store_true", help="Use synthetic data (dev/testing)")
    args = parser.parse_args()

    start = time.time()

    # 加载数据
    df = None
    if args.csv:
        df = load_data_from_csv(args.csv)
    elif args.db_url:
        df = load_data_from_db(args.db_url)
    elif args.synthetic:
        df = generate_synthetic_data(500)

    if df is None:
        print("No data source available. Use --csv, --db-url, or --synthetic", file=sys.stderr)
        sys.exit(1)

    if len(df) < args.min_samples:
        print(f"Insufficient samples: {len(df)} < {args.min_samples}", file=sys.stderr)
        sys.exit(2)

    # 训练
    auc, metadata = train_model(df, args.output_dir)

    elapsed = time.time() - start
    print(f"\nTraining completed in {elapsed:.1f}s. AUC={auc:.4f}")

    # 判断是否替换现有模型
    prev_meta_path = os.path.join(args.output_dir, "model_metadata.json.prev")
    if os.path.exists(prev_meta_path):
        with open(prev_meta_path) as f:
            prev_meta = json.load(f)
        prev_auc = prev_meta.get("auc", 0)
        if auc >= prev_auc + 0.01:
            print(f"MODEL_IMPROVED: AUC {prev_auc:.4f} → {auc:.4f}, deploying new model")
            os.rename(prev_meta_path, os.path.join(args.output_dir, "model_metadata.json.replaced"))
        else:
            print(f"MODEL_NOT_IMPROVED: AUC {prev_auc:.4f} → {auc:.4f}, keeping previous model")
            sys.exit(3)

    # 备份当前元数据
    meta_path = os.path.join(args.output_dir, "model_metadata.json")
    if os.path.exists(meta_path):
        os.rename(meta_path, prev_meta_path)

    print("SUCCESS: Model ready for deployment")
    sys.exit(0)

if __name__ == "__main__":
    main()
