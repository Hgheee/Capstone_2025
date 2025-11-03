#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
LOST112 API 연결 테스트 스크립트
"""
import os
import requests
from dotenv import load_dotenv
from datetime import date

# .env 로드
load_dotenv('.env')

# API 키 가져오기
api_key = os.getenv('DATA_GO_KR_SERVICE_KEY')

if not api_key:
    print("❌ 오류: DATA_GO_KR_SERVICE_KEY가 .env 파일에 없습니다!")
    print("\n.env 파일에 다음을 추가하세요:")
    print("DATA_GO_KR_SERVICE_KEY=여기에_API키_입력")
    exit(1)

print(f"✅ API 키 로드됨: {api_key[:20]}...{api_key[-10:]}")
print()

# 오늘 날짜
today = date.today().strftime("%Y%m%d")

# 테스트 URL (최소한의 데이터만)
url = (
    f"http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd"
    f"?serviceKey={api_key}"
    f"&START_YMD={today}"
    f"&END_YMD={today}"
    f"&NUM_OF_ROWS=5"
    f"&pageNo=1"
    f"&LST_LCT_CD=01"
    f"&_type=json"
)

print(f"🔍 테스트 URL:")
print(f"{url[:100]}...")
print()

print("📡 API 서버 연결 중...")
try:
    response = requests.get(url, timeout=30)
    print(f"✅ HTTP Status: {response.status_code}")
    
    if response.status_code == 200:
        print("✅ 연결 성공!")
        
        try:
            data = response.json()
            print(f"✅ JSON 파싱 성공!")
            
            # 응답 구조 확인
            if 'response' in data:
                body = data.get('response', {}).get('body', {})
                items = body.get('items', {})
                total = body.get('totalCount', 0)
                
                print(f"\n📊 데이터 요약:")
                print(f"  - 총 항목 수: {total}")
                
                if isinstance(items, dict) and 'item' in items:
                    item_list = items['item']
                    if isinstance(item_list, dict):
                        item_list = [item_list]
                    print(f"  - 받은 항목 수: {len(item_list)}")
                    
                    if item_list:
                        print(f"\n📦 첫 번째 항목:")
                        first = item_list[0]
                        print(f"  - 제목: {first.get('fdPrdtNm', 'N/A')}")
                        print(f"  - 장소: {first.get('depPlace', 'N/A')}")
                        print(f"  - 습득일: {first.get('fdYmd', 'N/A')}")
                        
                print("\n✅ API가 정상 작동합니다!")
                print("\n다음 명령으로 데이터를 수집하세요:")
                print("python lost112_collect_and_sync_fast.py --rows-per-page 100 --max-pages 10 --timeout 30")
            else:
                print(f"\n⚠️ 예상치 못한 응답 구조:")
                print(data)
        except Exception as e:
            print(f"\n❌ JSON 파싱 실패: {e}")
            print(f"응답 내용 (처음 500자):")
            print(response.text[:500])
    else:
        print(f"❌ HTTP 오류 발생")
        print(f"응답 내용: {response.text[:500]}")
        
except requests.exceptions.Timeout:
    print("❌ 타임아웃: API 서버 응답이 너무 느립니다.")
    print("\n해결 방법:")
    print("1. 인터넷 연결 확인")
    print("2. 나중에 다시 시도")
    print("3. 타임아웃 값을 더 늘리기 (--timeout 60)")
    
except requests.exceptions.ConnectionError as e:
    print(f"❌ 연결 오류: {e}")
    print("\n해결 방법:")
    print("1. 인터넷 연결 확인")
    print("2. 방화벽 설정 확인")
    print("3. VPN 사용 시 비활성화 후 재시도")
    
except Exception as e:
    print(f"❌ 오류 발생: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "="*60)

