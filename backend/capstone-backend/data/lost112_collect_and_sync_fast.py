#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LOST112 빠른 수집+동기화(임시테이블) 파이프라인
- 페이지 병렬 수집(기본 2동시), in-memory 변환, MySQL batch UPSERT
- STDOUT: 최종 요약 JSON (Spring에서 파싱하기 좋게)
- STDERR: 진행 로그
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

# MySQL connector는 외부 환경에 설치되어 있어야 합니다.
try:
    import mysql.connector  # type: ignore
except Exception:  # pragma: no cover
    mysql = None

BASE_URL = "http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd"

DEFAULT_REGION_CODE = "01"     # 서울
DEFAULT_ROWS = 100             # 페이지당 최대
DEFAULT_MAX_PAGES = 2          # 30초 제한 대응: 기본 2페이지만
DEFAULT_CONCURRENCY = 2        # 동시 요청 2개 (API 부하 고려)
DEFAULT_SLEEP_BETWEEN_BATCH = 0.0
DEFAULT_TIMEOUT_SEC = 12       # 각 요청 타임아웃
DEFAULT_ENV_FILE = ".env"
DEFAULT_TABLE = "lost_items_temp"
DEFAULT_BATCH_SIZE = 1000      # executemany 배치 크기
DEFAULT_DAY_RANGE = 1          # 기본 하루만

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
    # 소규모 재시도 설정
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
        # JSON 파싱 실패 시 텍스트 일부 로깅
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
    # 표준 컬럼 생성
    out = pd.DataFrame()
    get = df.get  # type: ignore

    out["item_id"]       = get("atcId", "")
    out["title"]         = get("fdPrdtNm", "")
    out["found_date"]    = get("fdYmd", "")
    out["storage_place"] = get("depPlace", "")
    out["image_url"]     = get("fdFilePathImg", "")
    out["color"]         = get("clrNm", "")
    out["description"]   = get("fdSbjt", "")
    prdt = get("prdtClNm", "")

    # "휴대폰 > 아이폰" → 상/하위 분리
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
        out["category"]    = [c[0] for c in cats]
        out["subcategory"] = [c[1] for c in cats]
    else:
        out["category"]    = ""
        out["subcategory"] = ""

    out["category_raw"]  = prdt

    # NaN → None
    out = out.where(pd.notnull(out), None)

    # 날짜 포맷 정리(벡터화)
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
        raise RuntimeError("mysql.connector 가 설치되어 있지 않습니다. (pip install mysql-connector-python)")

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

    # 튜플 변환(최소한의 슬라이싱으로 성능↑)
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
    p = argparse.ArgumentParser(description="LOST112 빠른 수집 후 MySQL 동기화")
    p.add_argument("--start-ymd", help="YYYYMMDD", default=None)
    p.add_argument("--end-ymd", help="YYYYMMDD", default=None)
    p.add_argument("--region-code", default=DEFAULT_REGION_CODE)
    p.add_argument("--rows-per-page", type=int, default=DEFAULT_ROWS)
    p.add_argument("--max-pages", type=int, default=DEFAULT_MAX_PAGES)
    p.add_argument("--concurrency", type=int, default=DEFAULT_CONCURRENCY)
    p.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT_SEC, help="각 요청 타임아웃(초)")
    p.add_argument("--env-file", default=DEFAULT_ENV_FILE, help=".env 경로 (서비스키/DB 설정)")
    p.add_argument("--service-key", default=None, help="DATA_GO_KR_SERVICE_KEY (우선)")
    p.add_argument("--table", default=DEFAULT_TABLE, help="적재 테이블명 (기본: lost_items_temp)")
    p.add_argument("--batch-size", type=int, default=DEFAULT_BATCH_SIZE, help="DB executemany 배치 크기")
    p.add_argument("--dry-run", action="store_true", help="DB 적재 생략(변환까지 수행)")
    args = p.parse_args()

    # 날짜 정규화
    s_ymd, e_ymd = normalize_dates(args.start_ymd, args.end_ymd)

    # .env 로드
    env_file = os.path.expanduser(args.env_file)
    if os.path.exists(env_file):
        load_dotenv(env_file)
    # 서비스키
    service_key = args.service_key or os.getenv("DATA_GO_KR_SERVICE_KEY")
    if not service_key:
        print(json.dumps({"success": False, "error": "DATA_GO_KR_SERVICE_KEY not set (.env or --service-key)"}))
        sys.exit(1)

    # 수집
    meta = Meta(
        start_ymd=s_ymd, end_ymd=e_ymd, region_code=args.region_code,
        rows_per_page=max(1, min(100, args.rows_per_page)),
        max_pages=max(1, args.max_pages), concurrency=max(1, args.concurrency)
    )

    t0 = time.perf_counter()
    session = requests_session(meta.concurrency)

    # 1) 첫 페이지 동기 요청 (totalCount 확인용)
    first_url = build_url(service_key, s_ymd, e_ymd, 1, meta.rows_per_page, meta.region_code)
    try:
        first = fetch_page(session, first_url, args.timeout)
    except Exception as e:
        print(json.dumps({"success": False, "error": f"fetch page1 failed: {e}"}))
        sys.exit(2)

    meta.total_count_header = first.get("totalCount")
    items_all: List[Dict[str, Any]] = list(first["items"])
    meta.fetched_pages = 1

    # 2) 나머지 페이지 병렬 수집
    from concurrent.futures import ThreadPoolExecutor, as_completed
    futures = []
    total_pages_to_fetch = max(1, meta.max_pages)
    if total_pages_to_fetch > 1:
        with ThreadPoolExecutor(max_workers=meta.concurrency) as ex:
            for page in range(2, total_pages_to_fetch + 1):
                url = build_url(service_key, s_ymd, e_ymd, page, meta.rows_per_page, meta.region_code)
                futures.append(ex.submit(fetch_page, session, url, args.timeout))
            for fut in as_completed(futures):
                try:
                    data = fut.result()
                    meta.fetched_pages += 1
                    items_all.extend(data["items"])
                except Exception as e:
                    log(f"[경고] 일부 페이지 수집 실패: {e}")

    meta.fetched_items = len(items_all)
    t1 = time.perf_counter()
    meta.elapsed_fetch_sec = round(t1 - t0, 3)

    # 3) 변환 (in-memory)
    if meta.fetched_items:
        df_raw = pd.json_normalize(items_all)
        df_std = transform_to_standard(df_raw)
    else:
        df_std = pd.DataFrame(columns=[
            "item_id","title","found_date","storage_place","image_url",
            "color","description","category","subcategory","category_raw"
        ])

    # 4) DB upsert (옵션)
    if args.dry_run:
        t2 = time.perf_counter()
        meta.elapsed_db_sec = 0.0
        meta.elapsed_total_sec = round(t2 - t0, 3)
        print(json.dumps({
            "success": True,
            "dryRun": True,
            "meta": meta.__dict__,
            "preview": df_std.head(3).to_dict(orient="records")
        }, ensure_ascii=False))
        return

    # DB env
    db_env = {
        "DB_HOST": os.getenv("DB_HOST", ""),
        "DB_PORT": os.getenv("DB_PORT", "3306"),
        "DB_USER": os.getenv("DB_USER", ""),
        "DB_PASSWORD": os.getenv("DB_PASSWORD", ""),
        "DB_NAME": os.getenv("DB_NAME", ""),
    }
    if not all([db_env["DB_HOST"], db_env["DB_USER"], db_env["DB_PASSWORD"], db_env["DB_NAME"]]):
        print(json.dumps({"success": False, "error": "DB env not set (DB_HOST/DB_USER/DB_PASSWORD/DB_NAME)"}))
        sys.exit(3)

    t2 = time.perf_counter()
    try:
        res = upsert_mysql(df_std, db_env, args.table, batch_size=max(100, args.batch_size))
        meta.upserted = int(res.get("affected") or 0)
    except Exception as e:
        print(json.dumps({"success": False, "error": f"DB upsert failed: {e}"}))
        sys.exit(4)
    t3 = time.perf_counter()
    meta.elapsed_db_sec = round(t3 - t2, 3)
    meta.elapsed_total_sec = round(t3 - t0, 3)

    print(json.dumps({
        "success": True,
        "meta": meta.__dict__,
    }, ensure_ascii=False))

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(json.dumps({"success": False, "error": "cancelled"}))
        sys.exit(130)
