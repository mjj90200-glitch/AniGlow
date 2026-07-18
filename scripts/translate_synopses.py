#!/usr/bin/env python3
"""
批量翻译动漫英文简介 → 中文
使用火山引擎 Ark API (doubao-seed-2.0-pro)
"""
import pymysql
import requests
import time
import sys
import os
from typing import Optional

# --- 配置 ---
DB_CONFIG = {
    "host": "localhost",
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.environ["MYSQL_PASSWORD"],
    "database": os.getenv("MYSQL_DATABASE", "aniglow"),
    "charset": "utf8mb4",
}

API_URL = os.getenv("AI_BASE_URL", "https://ark.cn-beijing.volces.com/api/coding/v3") + "/chat/completions"
API_KEY = os.environ["AI_API_KEY"]
MODEL = os.getenv("AI_MODEL", "doubao-seed-2.0-lite")


# --- 翻译函数 ---
def translate(text: str) -> Optional[str]:
    """调用火山引擎 Ark API 翻译英文到中文"""
    to_translate = text[:800] + "..." if len(text) > 800 else text

    payload = {
        "model": MODEL,
        "messages": [
            {
                "role": "system",
                "content": (
                    "你是一个专业动漫翻译。将英文动漫简介翻译成流畅简洁的中文。"
                    "直接输出中文译文，不要加任何解释或前缀。保持原文的分段和语气。"
                ),
            },
            {
                "role": "user",
                "content": f"翻译成中文，保持简洁：\n{to_translate}",
            },
        ],
        "max_tokens": 300,
        "temperature": 0.3,
    }

    try:
        resp = requests.post(
            API_URL,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {API_KEY}",
            },
            json=payload,
            timeout=30,
        )
        data = resp.json()
        if "choices" in data and len(data["choices"]) > 0:
            return data["choices"][0]["message"]["content"].strip()
        else:
            print(f"  API 返回异常: {data}")
            return None
    except Exception as e:
        print(f"  翻译请求失败: {e}")
        return None


# --- 数据库操作 ---
conn = pymysql.connect(**DB_CONFIG)
cursor = conn.cursor()

# 查询缺失中文简介的动漫
cursor.execute(
    "SELECT id, title, synopsis FROM anime "
    "WHERE synopsis_cn IS NULL AND synopsis IS NOT NULL AND CHAR_LENGTH(synopsis) > 30"
)
rows = cursor.fetchall()
total = len(rows)

print(f"发现 {total} 部动漫缺失中文简介\n")

if total == 0:
    print("所有动漫已有中文简介，无需修复。")
    cursor.close()
    conn.close()
    sys.exit(0)


# --- 批量翻译 ---
success = 0
fail = 0
skip = 0

for idx, (anime_id, title, synopsis) in enumerate(rows, 1):
    # 跳过非英文文本
    ascii_count = sum(1 for c in synopsis if ord(c) < 128)
    if ascii_count / len(synopsis) <= 0.7:
        skip += 1
        print(f"[{idx}/{total}] {title} — 跳过（非英文文本）")
        continue

    print(f"[{idx}/{total}] {title} — 翻译中...", end=" ", flush=True)

    cn = translate(synopsis)
    if cn:
        # 转义可能存在的特殊字符
        cursor.execute(
            "UPDATE anime SET synopsis_cn = %s WHERE id = %s",
            (cn, anime_id),
        )
        conn.commit()
        success += 1
        preview = cn[:50].replace("\n", " ")
        print(f"✓ {preview}...")
    else:
        fail += 1
        print("✗ 失败")

    # 控制频率：每秒最多 3 次请求
    time.sleep(0.4)

# --- 收尾 ---
cursor.close()
conn.close()

print(f"\n{'='*50}")
print(f"翻译完成！成功: {success}, 失败: {fail}, 跳过: {skip}")
print(f"总计处理: {total} 部")
