# /2025_capstone/data/transform_to_standard.py
# 목적:
# - 수집 CSV를 읽어 표준 스키마로 정리
# - prdtClNm을 상/하위 카테고리로 분해
# - 주요 컬럼만 추출/리네이밍

from pathlib import Path
import pandas as pd

def split_category(v: str):
    # "휴대폰 > 아이폰" → ("휴대폰", "아이폰")
    if not isinstance(v, str):
        return "", ""
    parts = [p.strip() for p in v.split(">")]
    if len(parts) >= 2:
        return parts[0], parts[1]
    elif len(parts) == 1:
        return parts[0], ""
    else:
        return "", ""

def main():
    here = Path(__file__).resolve().parent
    in_csv = here / "out" / "lost112_20250901_20250910_01.csv"  # 앞 단계에서 생성된 파일명과 동일하게
    out_csv = here / "out" / "lost112_20250901_20250910_01_std.csv"

    if not in_csv.exists():
        print(f"[오류] 입력 CSV가 없습니다: {in_csv}")
        return

    df = pd.read_csv(in_csv)

    # 표준 스키마 구성 (필요시 컬럼 추가/조정)
    # 원본 컬럼 후보: atcId, fdPrdtNm(=물품명), fdYmd(습득일), depPlace(보관장소), prdtClNm(카테고리),
    #                 fdFilePathImg(이미지), clrNm(색상), fdSbjt(설명) 등
    std = pd.DataFrame()
    std["item_id"]      = df.get("atcId", "")
    std["title"]        = df.get("fdPrdtNm", "")
    std["found_date"]   = df.get("fdYmd", "")
    std["storage_place"]= df.get("depPlace", "")
    std["image_url"]    = df.get("fdFilePathImg", "")
    std["color"]        = df.get("clrNm", "")
    std["description"]  = df.get("fdSbjt", "")
    # 카테고리 분리
    upper, lower = [], []
    for v in df.get("prdtClNm", []):
        u, l = split_category(v)
        upper.append(u)
        lower.append(l)
    std["category"]     = upper
    std["subcategory"]  = lower
    # 원본 카테고리 전체 문자열도 보관(디버깅용)
    std["category_raw"] = df.get("prdtClNm", "")

    std.to_csv(out_csv, index=False, encoding="utf-8-sig")
    print(f"[저장] 표준 스키마 CSV → {out_csv}")
    print("[미리보기 5행]")
    print(std.head(5).to_string(index=False))

if __name__ == "__main__":
    main()
