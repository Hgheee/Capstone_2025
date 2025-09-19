# /2025_capstone/data/load_mysql_temp.py
# 목적:
# - 표준 스키마 CSV(lost112_*_std.csv)를 읽어 MySQL의 lost_items_temp에 적재

from pathlib import Path
import os, sys
import pandas as pd
from dotenv import load_dotenv
import mysql.connector

def main():
    here = Path(__file__).resolve().parent
    env_path = here / ".env"
    in_csv = here / "out" / "lost112_20250901_20250910_01_std.csv"

    if not env_path.exists():
        print(f"[오류] .env 없음: {env_path}")
        sys.exit(1)
    load_dotenv(env_path)

    if not in_csv.exists():
        print(f"[오류] 입력 CSV 없음: {in_csv}")
        sys.exit(1)

    df = pd.read_csv(in_csv)

    conn = mysql.connector.connect(
        host=os.getenv("DB_HOST"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        database=os.getenv("DB_NAME"),
    )
    cursor = conn.cursor()

    # upsert를 원하면 ON DUPLICATE KEY UPDATE 구문 사용 가능
    sql = """
    INSERT INTO lost_items_temp
      (item_id, title, found_date, storage_place, image_url, color, description, category, subcategory, category_raw)
    VALUES
      (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
      title=VALUES(title),
      found_date=VALUES(found_date),
      storage_place=VALUES(storage_place),
      image_url=VALUES(image_url),
      color=VALUES(color),
      description=VALUES(description),
      category=VALUES(category),
      subcategory=VALUES(subcategory),
      category_raw=VALUES(category_raw);
    """

    # found_date가 YYYY-MM-DD 형식이 아니면 간단 변환
    def to_date(v):
        # 예: "2025-09-19" 또는 "20250919" 처리
        s = str(v)
        if len(s) == 8 and s.isdigit():
            return f"{s[0:4]}-{s[4:6]}-{s[6:8]}"
        return s if "-" in s else None

    rows = []
    for _, r in df.iterrows():
        rows.append((
            str(r.get("item_id", "")),
            str(r.get("title", ""))[:255],
            to_date(r.get("found_date", "")),
            str(r.get("storage_place", ""))[:255],
            str(r.get("image_url", ""))[:512],
            str(r.get("color", ""))[:100],
            None if pd.isna(r.get("description", "")) else str(r.get("description", "")),
            str(r.get("category", ""))[:100],
            str(r.get("subcategory", ""))[:100],
            str(r.get("category_raw", ""))[:255],
        ))

    try:
        cursor.executemany(sql, rows)
        conn.commit()
        print(f"[적재] {len(rows)}건 삽입/갱신 완료")
    except Exception as e:
        conn.rollback()
        print("[오류] 적재 실패:", e)
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    main()
