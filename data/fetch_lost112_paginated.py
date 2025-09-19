# /2025_capstone/data/fetch_lost112_paginated.py
# 목적:
# - getLosfundInfoAccTpNmCstdyPlace 엔드포인트로 다건(페이지네이션) 수집
# - 수집 후 하나의 CSV로 저장

from pathlib import Path
import os, sys, json, time
import requests
from dotenv import load_dotenv
import pandas as pd
import urllib.parse

BASE_URL = "http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd"

def build_url(service_key_raw: str, start_ymd: str, end_ymd: str, page_no: int, rows: int, region_code: str) -> str:
    # serviceKey URL 인코딩
    service_key = urllib.parse.quote_plus(service_key_raw)
    url = (
        f"{BASE_URL}"
        f"?serviceKey={service_key}"
        f"&START_YMD={start_ymd}"
        f"&END_YMD={end_ymd}"
        f"&NUM_OF_ROWS={rows}"
        f"&pageNo={page_no}"
        f"&LST_LCT_CD={region_code}"
        f"&_type=json"
    )
    return url

def main():
    here = Path(__file__).resolve().parent  # /data
    env_path = here / ".env"
    out_dir = here / "out"
    out_dir.mkdir(exist_ok=True)

    if not env_path.exists():
        print(f"[오류] .env가 없습니다: {env_path}")
        sys.exit(1)
    load_dotenv(env_path)

    service_key = os.getenv("DATA_GO_KR_SERVICE_KEY")
    if not service_key:
        print("[오류] DATA_GO_KR_SERVICE_KEY 누락")
        sys.exit(1)

    # 수집 파라미터 (원하면 자유롭게 조정)
    START_YMD = "20250901"
    END_YMD   = "20250910"
    LST_LCT_CD = "01"   # 서울 (가이드 기준)
    NUM_OF_ROWS = 50    # 10~100 권장
    MAX_PAGES = 5       # 테스트: 5페이지(=최대 250건). 필요 시 늘려도 됨.
    SLEEP_SEC = 0.3     # API 과속 방지

    all_rows = []
    total_count_seen = None

    for page in range(1, MAX_PAGES + 1):
        url = build_url(service_key, START_YMD, END_YMD, page, NUM_OF_ROWS, LST_LCT_CD)
        print(f"[요청] page={page} → {url}")

        # SSL 문제 회피: HTTP 사용(기본). 필요 시 timeout 늘림.
        try:
            resp = requests.get(url, timeout=60)
            resp.raise_for_status()
        except requests.exceptions.RequestException as e:
            print(f"[오류] 요청 실패(page {page}):", e)
            break

        # raw 저장(선택)
        raw_path = out_dir / f"lost112_raw_p{page}.json"
        with raw_path.open("w", encoding="utf-8") as f:
            f.write(resp.text)

        # JSON 파싱
        try:
            data = resp.json()
        except Exception:
            print("[오류] JSON 파싱 실패. 응답 일부:", resp.text[:300])
            break

        body = data.get("response", {}).get("body", {})
        total_count = body.get("totalCount")
        if total_count_seen is None:
            total_count_seen = total_count
        items_container = body.get("items", {})
        items = items_container.get("item", [])
        if isinstance(items, dict):
            items = [items]

        print(f"[정보] page {page} 수신 {len(items)}건 / totalCount={total_count}")
        if not items:
            print("[정보] 더 이상 항목이 없습니다. 중단.")
            break

        # pandas로 적재
        df = pd.json_normalize(items)
        all_rows.append(df)

        time.sleep(SLEEP_SEC)

    if not all_rows:
        print("[주의] 수집된 데이터가 없습니다.")
        return

    final_df = pd.concat(all_rows, ignore_index=True)

    # 최종 CSV 저장
    csv_path = out_dir / f"lost112_{START_YMD}_{END_YMD}_{LST_LCT_CD}.csv"
    final_df.to_csv(csv_path, index=False, encoding="utf-8-sig")
    print(f"[저장] 수집 CSV → {csv_path}")
    print("[미리보기 5행]")
    print(final_df.head(5).to_string(index=False))
    print(f"[요약] 총 {len(final_df)}건 수집 / totalCount 첫페이지 기준={total_count_seen}")

if __name__ == "__main__":
    main()
