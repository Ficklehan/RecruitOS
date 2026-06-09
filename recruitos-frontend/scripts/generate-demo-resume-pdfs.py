#!/usr/bin/env python3
"""Generate demo resume PDFs for public/demo/resumes/."""
from pathlib import Path

from fpdf import FPDF

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "public" / "demo" / "resumes"
FONT = Path("/System/Library/Fonts/Supplemental/Arial Unicode.ttf")

RESUMES = {
    "zhangwei.pdf": ["张伟 | 高级前端工程师", "阿里巴巴 · 6年经验", "北京大学 · 计算机硕士", "技能: Vue3, TypeScript, React, 微前端"],
    "lina.pdf": ["李娜 | 前端工程师", "腾讯 IEG · 4年经验", "浙江大学 · 软件工程本科", "技能: Vue3, JavaScript, Webpack"],
    "chenhao.pdf": ["陈浩 | 前端架构师", "百度 · 8年经验", "武汉大学 · 计算机本科", "技能: Vue3, React, 工程化"],
    "liuyang.pdf": ["刘洋 | 数据分析师", "京东 · 4年经验", "中科院 · 统计学硕士", "技能: SQL, Python, Tableau"],
    "zhaomin.pdf": ["赵敏 | 高级产品经理", "美团 · 7年经验", "复旦大学 · 信息管理硕士", "技能: B端产品, HR SaaS"],
    "mali.pdf": ["马丽 | 前端工程师", "蚂蚁集团 · 5年经验", "上海交大 · 软件工程硕士", "技能: React, Vue3"],
    "wangqiang.pdf": ["王强 | Java工程师", "字节跳动 · 5年经验", "清华大学 · 计算机本科", "技能: Java, Spring Boot"],
    "xuming.pdf": ["徐明 | 后端开发", "腾讯 · 4年经验", "北航 · 计算机硕士", "技能: Java, 微服务, Redis"],
    "yangfan.pdf": ["杨帆 | 前端开发", "小米 · 3年经验", "北邮 · 通信工程本科", "技能: Vue3, JavaScript"],
    "wulei.pdf": ["吴磊 | 前端架构师", "华为 · 10年经验", "中科大 · 计算机硕士", "技能: 前端架构, Vue3, 微前端"],
    "zhengxue.pdf": ["郑雪 | 产品经理", "滴滴 · 4年经验", "人大 · 工商管理本科", "技能: 产品设计, 用户研究"],
    "sunchao.pdf": ["孙超 | Java高级开发", "快手 · 6年经验", "北理工 · 计算机本科", "技能: Java, 高并发"],
    "huangtao.pdf": ["黄涛 | 数据工程师", "拼多多 · 3年经验", "华科 · 数据科学本科", "技能: SQL, Spark, Hive"],
    "linlin.pdf": ["林琳 | UI设计师", "字节跳动 · 5年经验", "央美 · 视觉传达本科", "技能: Figma, Sketch"],
    "zhouting.pdf": ["周婷 | Java开发", "网易 · 5年经验", "南开大学 · 软件工程本科", "技能: Java, Spring"],
}


def build_pdf(lines: list[str]) -> FPDF:
    pdf = FPDF()
    pdf.set_margins(18, 18, 18)
    pdf.set_auto_page_break(auto=True, margin=18)
    pdf.add_page()
    pdf.add_font("zh", "", str(FONT))
    pdf.set_font("zh", size=16)
    pdf.cell(0, 12, "RecruitOS 演示简历", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(4)
    pdf.set_font("zh", size=12)
    for line in lines:
        pdf.multi_cell(0, 8, line)
        pdf.ln(2)
    pdf.ln(4)
    pdf.set_font("zh", size=10)
    pdf.multi_cell(0, 6, "本文件为演示数据，用于验证简历预览与下载功能。")
    return pdf


def main():
    if not FONT.exists():
        raise SystemExit(f"Font not found: {FONT}")
    OUT.mkdir(parents=True, exist_ok=True)
    for fname, lines in RESUMES.items():
        build_pdf(lines).output(str(OUT / fname))
        print(f"created {fname}")
    print(f"done: {len(list(OUT.glob('*.pdf')))} files in {OUT}")


if __name__ == "__main__":
    main()
