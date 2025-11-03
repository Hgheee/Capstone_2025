#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
서울시 API 연결 테스트
"""
import requests
import os
from dotenv import load_dotenv

load_dotenv('.env')

SEOUL_API_KEY = os.getenv('SEOUL_API_KEY', '')

print("=" * 60)
print("🧪 서울시 분실물 API 테스트")
print("=" * 60)
print()

if not SEOUL_API_KEY or SEOUL_API_KEY == 'your_seoul_api_key_here':
    print("❌ SEOUL_API_KEY가 .env 파일에 없습니다!")
    print()
    print(".env 파일에 추가하세요:")
    print("SEOUL_API_KEY=여기에_실제_API키")
    print()
    print("API 키 발급: https://data.seoul.go.kr")
    exit(1)

print(f"✅ API 키 확인: {SEOUL_API_KEY[:15]}...{SEOUL_API_KEY[-10:]}")
print()

# 테스트 URL 구성
url = f"http://openapi.seoul.go.kr:8088/{SEOUL_API_KEY}/json/lostArticleInfo/1/5"

print(f"📡 테스트 URL:")
print(f"{url}")
print()

print("서버 연결 중...")
try:
    response = requests.get(url, timeout=10)
    
    print(f"✅ HTTP Status: {response.status_code}")
    print()
    
    if response.status_code != 200:
        print(f"❌ HTTP 오류!")
        print(f"응답: {response.text[:500]}")
        exit(1)
    
    # Content-Type 확인
    content_type = response.headers.get('Content-Type', '')
    print(f"Content-Type: {content_type}")
    print()
    
    # JSON 파싱 시도
    try:
        data = response.json()
        print("✅ JSON 파싱 성공!")
        print()
        
        # 응답 구조 확인
        if 'lostArticleInfo' in data:
            info = data['lostArticleInfo']
            result = info.get('RESULT', {})
            
            print(f"📊 API 응답:")
            print(f"  CODE: {result.get('CODE')}")
            print(f"  MESSAGE: {result.get('MESSAGE')}")
            print()
            
            if result.get('CODE') == 'INFO-000':
                rows = info.get('row', [])
                print(f"✅ 데이터 수신 성공!")
                print(f"  항목 수: {len(rows)}")
                
                if rows:
                    print()
                    print("📦 첫 번째 항목:")
                    first = rows[0]
                    print(f"  관리번호: {first.get('LOST_MNG_NO')}")
                    print(f"  제목: {first.get('LOST_NM')}")
                    print(f"  분류: {first.get('LOST_KND')}")
                    print(f"  습득장소: {first.get('RCPL')}")
                    print(f"  보관장소: {first.get('CSTD_PLC')}")
                    print()
                
                print("=" * 60)
                print("✅ API가 정상 작동합니다!")
                print()
                print("다음 명령으로 데이터를 수집하세요:")
                print("python collect_seoul_data.py")
                print("=" * 60)
            else:
                print(f"❌ API 오류!")
                print(f"  CODE: {result.get('CODE')}")
                print(f"  MESSAGE: {result.get('MESSAGE')}")
                print()
                print("가능한 원인:")
                print("1. API 키가 잘못됨")
                print("2. API 키가 활성화되지 않음")
                print("3. 일일 호출 한도 초과")
        else:
            print("⚠️ 예상치 못한 응답 구조:")
            print(data)
            
    except ValueError as e:
        print(f"❌ JSON 파싱 실패!")
        print(f"오류: {e}")
        print()
        print("응답 내용 (처음 500자):")
        print(response.text[:500])
        print()
        print("가능한 원인:")
        print("1. API 키가 잘못됨 (HTML 에러 페이지 반환)")
        print("2. URL이 잘못됨")
        print("3. 서버 점검 중")
        
except requests.exceptions.Timeout:
    print("❌ 타임아웃!")
    print("서버 응답이 너무 느립니다.")
    
except requests.exceptions.ConnectionError as e:
    print(f"❌ 연결 오류!")
    print(f"{e}")
    print()
    print("가능한 원인:")
    print("1. 인터넷 연결 문제")
    print("2. 방화벽 차단")
    
except Exception as e:
    print(f"❌ 예상치 못한 오류!")
    print(f"{e}")
    import traceback
    traceback.print_exc()

print()

