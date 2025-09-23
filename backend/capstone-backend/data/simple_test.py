#!/usr/bin/env python3
import sys
import json
import argparse
from datetime import datetime

def main():
    # 기본값으로도 작동하도록 설정
    parser = argparse.ArgumentParser(description='LOST112 테스트 수집')
    parser.add_argument('--start-ymd', default='20250920', help='시작 날짜')
    parser.add_argument('--end-ymd', default='20250923', help='종료 날짜') 
    parser.add_argument('--max-pages', type=int, default=2, help='최대 페이지')
    parser.add_argument('--concurrency', type=int, default=2, help='동시 처리')
    parser.add_argument('--region-code', default='01', help='지역 코드')
    parser.add_argument('--rows-per-page', type=int, default=100, help='페이지당 행 수')
    
    # 모든 인수 허용 (알 수 없는 인수 무시)
    args, unknown = parser.parse_known_args()
    
    # STDERR로 디버그 정보 출력
    print(f"Python 스크립트 시작: {datetime.now()}", file=sys.stderr)
    print(f"인수: start-ymd={args.start_ymd}, end-ymd={args.end_ymd}, max-pages={args.max_pages}", file=sys.stderr)
    print(f"알 수 없는 인수: {unknown}", file=sys.stderr)
    
    # 성공 응답 생성
    result = {
        "success": True,
        "status": "completed", 
        "meta": {
            "start_ymd": args.start_ymd,
            "end_ymd": args.end_ymd,
            "max_pages": args.max_pages,
            "concurrency": args.concurrency,
            "fetched_pages": args.max_pages,
            "fetched_items": 150,
            "upserted": 150,
            "elapsed_total_sec": 2.5,
            "elapsed_fetch_sec": 1.8,
            "elapsed_db_sec": 0.7,
            "script": "simple_test.py",
            "timestamp": datetime.now().isoformat(),
            "python_version": sys.version,
            "received_args": sys.argv[1:]
        },
        "items": []
    }
    
    # 150개 테스트 아이템 생성
    for i in range(150):
        item = {
            "atcId": f"SIMPLE{i:03d}",
            "fdPrdtNm": f"간단 테스트 아이템 {i+1}",
            "fdYmd": args.end_ymd,
            "depPlace": "서울지방경찰청",
            "prdtClNm": "전자제품",
            "clrNm": ["검정색", "흰색", "파란색", "빨간색"][i % 4],
            "fdSbjt": f"테스트용 분실물 설명 {i+1}",
            "fdFilePathImg": f"https://example.com/test{i}.jpg"
        }
        result["items"].append(item)
    
    # STDOUT으로 JSON 결과 출력 (Spring에서 파싱)
    print(json.dumps(result, ensure_ascii=False))
    
    # STDERR로 완료 메시지
    print(f"테스트 스크립트 정상 완료: 150개 아이템 생성", file=sys.stderr)
    
    return 0

if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except Exception as e:
        print(f"스크립트 실행 중 예외: {str(e)}", file=sys.stderr)
        error_result = {
            "success": False,
            "error": str(e),
            "items": []
        }
        print(json.dumps(error_result))
        sys.exit(1)
