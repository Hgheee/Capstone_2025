#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LOST112 빠른 수집+동기화(임시테이블) 파이프라인
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.parse
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Any, Dict, List, Optional, Tuple

import pandas as pd
import requests
from dotenv import load_dotenv

try:
    import mysql.connector  # type: ignore
except Exception:
    mysql = None

BASE_URL = "http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd"

DEFAULT_REGION_CODE = ""       # ✅ 전국 (빈 문자열)
DEFAULT_ROWS = 100
DEFAULT_MAX_PAGES = 2
DEFAULT_CONCURRENCY = 2
DEFAULT_SLEEP_BETWEEN_BATCH = 0.0
DEFAULT_TIMEOUT_SEC = 12
DEFAULT_ENV_FILE = ".env"
DEFAULT_TABLE = "lost_items_temp"
DEFAULT_BATCH_SIZE = 1000
DEFAULT_DAY_RANGE = 1

def log(msg: str) -> None:
    print(msg, file=sys.stderr)

def build_url(service_key: str, start_ymd: str, end_ymd: str, page_no: int, rows: int, region_code: str) -> str:
    sk = urllib.parse.quote_plus(service_key)
    return (
        f"{BASE_URL}"
        f"?serviceKey={sk}&START_YMD={start_ymd}&END_YMD={end_ymd}"
        f"&NUM_OF_ROWS={rows}&pageNo={page_no}&LST_LCT_CD={region_code}&_type=json"
    )

def normalize_dates(start_ymd: Optional[str], end_ymd: Optional[str]) -> Tuple[str, str]:
    today = date.today()
    if not start_ymd and not end_ymd:
        s = today.strftime("%Y%m%d")
        return (s, s)
    if not start_ymd:
        start_ymd = end_ymd
    if not end_ymd:
        end_ymd = start_ymd
    if start_ymd > end_ymd:
        start_ymd, end_ymd = end_ymd, start_ymd
    return start_ymd, end_ymd

def requests_session(concurrency: int = DEFAULT_CONCURRENCY) -> requests.Session:
    from requests.adapters import HTTPAdapter
    from urllib3.util.retry import Retry
    session = requests.Session()
    retries = Retry(total=2, backoff_factor=0.2, status_forcelist=[429, 500, 502, 503, 504])
    adapter = HTTPAdapter(pool_connections=max(4, concurrency*2), pool_maxsize=max(8, concurrency*4), max_retries=retries)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session

def fetch_page(session: requests.Session, url: str, timeout: float) -> Dict[str, Any]:
    resp = session.get(url, timeout=timeout)
    resp.raise_for_status()
    try:
        data = resp.json()
    except Exception:
        txt = resp.text[:300]
        raise RuntimeError(f"JSON decode failed: {txt}")
    body = (data.get("response") or {}).get("body") or {}
    items = (body.get("items") or {}).get("item") or []
    if isinstance(items, dict):
        items = [items]
    return {
        "totalCount": body.get("totalCount"),
        "items": items,
    }

def transform_to_standard(df: pd.DataFrame) -> pd.DataFrame:
    out = pd.DataFrame()
    get = df.get

    out["item_id"] = get("atcId", "")
    out["title"] = get("fdPrdtNm", "")
    out["found_date"] = get("fdYmd", "")
    out["storage_place"] = get("depPlace", "")
    out["image_url"] = get("fdFilePathImg", "")
    out["color"] = get("clrNm", "")
    out["description"] = get("fdSbjt", "")
    prdt = get("prdtClNm", "")

    def split_cat(v: Any) -> Tuple[str, str]:
        if not isinstance(v, str):
            return "", ""
        parts = [p.strip() for p in v.split(">")]
        if len(parts) >= 2:
            return parts[0], parts[1]
        if len(parts) == 1:
            return parts[0], ""
        return "", ""

    cats = prdt.apply(split_cat) if hasattr(prdt, "apply") else []
    if len(cats) > 0:
        out["category"] = [c[0] for c in cats]
        out["subcategory"] = [c[1] for c in cats]
    else:
        out["category"] = ""
        out["subcategory"] = ""

    out["category_raw"] = prdt
    out = out.where(pd.notnull(out), None)

    def to_ymd(s: Any) -> Optional[str]:
        if s is None:
            return None
        s = str(s)
        if len(s) == 8 and s.isdigit():
            return f"{s[0:4]}-{s[4:6]}-{s[6:8]}"
        if "-" in s:
            return s
        return None
    out["found_date"] = out["found_date"].map(to_ymd)
    return out

def chunk_iter(rows: List[Tuple], size: int) -> List[List[Tuple]]:
    return [rows[i:i+size] for i in range(0, len(rows), size)]

def upsert_mysql(df: pd.DataFrame, env: Dict[str, str], table: str, batch_size: int = DEFAULT_BATCH_SIZE) -> Dict[str, Any]:
    if mysql is None:
        raise RuntimeError("mysql.connector not installed")

    conn = mysql.connector.connect(
        host=env.get("DB_HOST"),
        port=int(env.get("DB_PORT", "3306")),
        user=env.get("DB_USER"),
        password=env.get("DB_PASSWORD"),
        database=env.get("DB_NAME"),
        autocommit=False,
    )
    cursor = conn.cursor()

    sql = f"""
    INSERT INTO {table}
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
      category_raw=VALUES(category_raw)
    """

    to_tuple = df[["item_id","title","found_date","storage_place","image_url","color","description","category","subcategory","category_raw"]]\
                 .itertuples(index=False, name=None)
    rows = list(to_tuple)

    inserted = 0
    try:
        for batch in chunk_iter(rows, batch_size):
            cursor.executemany(sql, batch)
            inserted += len(batch)
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise
    finally:
        cursor.close()
        conn.close()
    return {"affected": inserted}

@dataclass
class Meta:
    start_ymd: str
    end_ymd: str
    region_code: str
    rows_per_page: int
    max_pages: int
    concurrency: int
    fetched_pages: int = 0
    fetched_items: int = 0
    total_count_header: Optional[int] = None
    upserted: int = 0
    elapsed_fetch_sec: float = 0.0
    elapsed_db_sec: float = 0.0
    elapsed_total_sec: float = 0.0

def main() -> None:
    p = argparse.ArgumentParser(description="LOST112 수집")
    p.add_argument("--start-ymd", help="YYYYMMDD", default=None)
    p.add_argument("--end-ymd", help="YYYYMMDD", default=None)
    p.add_argument("--region-code", default=DEFAULT_REGION_CODE)
    p.add_argument("--rows-per-page", type=int, default=DEFAULT_ROWS)
    p.add_argument("--max-pages", type=int, default=DEFAULT_MAX_PAGES)
    p.add_argument("--concurrency", type=int, default=DEFAULT_CONCURRENCY)
    p.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_SEC)
    p.add_argument("--env-file", default=DEFAULT_ENV_FILE)
    p.add_argument("--service-key", default=None)
    p.add_argument("--table", default=DEFAULT_TABLE)
    p.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE)
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()

    s_ymd, e_ymd = normalize_dates(args.start_ymd, args.end_ymd)

    env_file = os.path.expanduser(args.env_file)
    if os.path.exists(env_file):
        load_dotenv(env_file)
    service_key = args.service_key or os.getenv("DATA_GO_KR_SERVICE_KEY")
    if not service_key:
        print(json.dumps({"success": False, "error": "SERVICE_KEY not set"}))
        sys.exit(1)

    meta = Meta(
        start_ymd=s_ymd, end_ymd=e_ymd, region_code=args.region_code,
        rows_per_page=max(1, min(100, args.rows_per_page)),
        max_pages=max(1, args.max_pages), concurrency=max(1, args.concurrency)
    )

    t0 = time.perf_counter()
    session = requests_session(meta.concurrency)

    first_url = build_url(service_key, s_ymd, e_ymd, 1, meta.rows_per_page, meta.region_code)
    try:
        first = fetch_page(session, first_url, args.timeout)
    except Exception as e:
        print(json.dumps({"success": False, "error": f"fetch failed: {e}"}))
        sys.exit(2)

    meta.total_count_header = first.get("totalCount")
    items_all: List[Dict[str, Any]] = list(first["items"])
    meta.fetched_pages = 1

    from concurrent.futures import ThreadPoolExecutor, as_completed
    futures = []
    total_pages_to_fetch = max(1, meta.max_pages)
    if total_pages_to_fetch > 1:
        with ThreadPoolExecutor(max_workers=meta.concurrency) as ex:
            for page in range(2, total_pages_to_fetch + 1):
                url = build_url(service_key)
