# /2025_capstone/data/api_test.py
# 목적:
# - 경찰청_습득물정보 조회 서비스 호출 (공공데이터포털)
# - JSON → CSV 변환
# - 요청 URL 출력 (검증용)

from pathlib import Path
import os
import sys
import json
import requests
from dotenv import load_dotenv
import pandas as pd
import urllib.parse

# 엔드포인트 (지역+기간별 습득물 조회)
BASE_URL = "http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccTpNmCstdyPlace"

def main():
    here = Path(__file__).resolve().parent
    env_path = here / ".env"
    out_dir = here / "out"
    out_dir.mkdir(exist_ok=True)

    # 1) 환경변수 로드
    if not env_path.exists():
        print(f"[오류] 환경파일(.env)이 없습니다: {env_path}")
        sys.exit(1)
    load_dotenv(env_path)

    service_key_raw = os.getenv("DATA_GO_KR_SERVICE_KEY")
    if not service_key_raw:
        print("[오류] .env에 DATA_GO_KR_SERVICE_KEY가 없습니다.")
        sys.exit(1)

    # 2) URL 인코딩 처리
    service_key = urllib.parse.quote_plus(service_key_raw)

    # 3) 요청 URL 직접 구성 (params 대신 f-string으로)
    url = (
        f"{BASE_URL}"
        f"?serviceKey={service_key}"
        f"&START_YMD=20250901"
        f"&END_YMD=20250910"
        f"&NUM_OF_ROWS=10"
        f"&pageNo=1"
        f"&LST_LCT_CD=01"   # 서울 
        f"&_type=json"
    )

    print("[최종 요청 URL]", url)

    # 4) API 호출
    try:
        resp = requests.get(url, timeout=60)
        resp.raise_for_status()
    except requests.exceptions.RequestException as e:
        print("[오류] API 요청 실패:", e)
        sys.exit(1)

    # 5) JSON 파싱
    try:
        data = resp.json()
    except json.JSONDecodeError:
        print("[오류] JSON 파싱 실패. 응답 내용 일부:")
        print(resp.text[:500])
        sys.exit(1)

    # 6) RAW JSON 저장
    raw_path = out_dir / "lost112_sample_raw.json"
    with raw_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"[저장] RAW JSON → {raw_path}")

    # 7) items 추출
    body = data.get("response", {}).get("body", {})
    items_container = body.get("items", {})
    items = items_container.get("item", [])

    if isinstance(items, dict):  # 단일 항목 처리
        items = [items]

    print(f"[정보] 수신 item 개수: {len(items)} / totalCount: {body.get('totalCount')}")

    # 8) DataFrame 변환 → CSV
    if len(items) > 0:
        df = pd.json_normalize(items)
        csv_path = out_dir / "lost112_sample.csv"
        df.to_csv(csv_path, index=False, encoding="utf-8-sig")
        print(f"[저장] CSV → {csv_path}")
        print("[미리보기 5행]")
        print(df.head(5).to_string(index=False))
    else:
        print("[주의] items가 비어 있음. 날짜/지역 코드를 바꿔보세요.")

if __name__ == "__main__":
    main()
