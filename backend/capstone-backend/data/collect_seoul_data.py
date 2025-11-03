#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
서울시 분실물 데이터 수집 스크립트
서울 열린데이터광장 API 사용
"""
import requests
import mysql.connector
import os
from dotenv import load_dotenv
import time

# .env 로드
load_dotenv('.env')

# API 설정
SEOUL_API_KEY = os.getenv('SEOUL_API_KEY', '')
if not SEOUL_API_KEY or SEOUL_API_KEY == 'your_seoul_api_key_here':
    print("❌ SEOUL_API_KEY가 설정되지 않았습니다!")
    print("\n.env 파일에 다음을 추가하세요:")
    print("SEOUL_API_KEY=여기에_실제_API키")
    print("\nAPI 키 발급: https://data.seoul.go.kr")
    exit(1)

BASE_URL = f"http://openapi.seoul.go.kr:8088/{SEOUL_API_KEY}/json/lostArticleInfo"

# MySQL 설정
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'user': os.getenv('DB_USER', 'hogeonhee'),
    'password': os.getenv('DB_PASSWORD', '0316'),
    'database': os.getenv('DB_NAME', 'capstone_db'),
    'charset': 'utf8mb4',  # UTF-8 인코딩 설정
    'use_unicode': True,   # 유니코드 사용
}

print(f"✅ API 키: {SEOUL_API_KEY[:10]}...")
print(f"✅ DB: {DB_CONFIG['database']}@{DB_CONFIG['host']}")
print()

def fetch_page(start, end):
    """서울시 API에서 데이터 가져오기"""
    url = f"{BASE_URL}/{start}/{end}"
    print(f"📡 요청: {start}-{end}번 항목")
    print(f"   URL: {url}")
    
    try:
        response = requests.get(url, timeout=15)
        print(f"   Status: {response.status_code}")
        
        # 응답 내용 확인
        if response.status_code != 200:
            print(f"   ❌ HTTP 오류: {response.status_code}")
            print(f"   응답: {response.text[:200]}")
            return []
        
        # JSON 파싱 시도
        try:
            data = response.json()
        except ValueError as e:
            print(f"   ❌ JSON 파싱 실패: {e}")
            print(f"   응답 내용: {response.text[:500]}")
            return []
        
        if 'lostArticleInfo' in data:
            info = data['lostArticleInfo']
            result = info.get('RESULT', {})
            
            if result.get('CODE') != 'INFO-000':
                msg = result.get('MESSAGE', 'Unknown')
                if 'DATA_NOT_FOUND' in result.get('CODE', ''):
                    return None  # 더 이상 데이터 없음
                print(f"⚠️ API 오류: {msg}")
                return []
            
            rows = info.get('row', [])
            print(f"   ✅ {len(rows)}개 수신")
            return rows
        else:
            print("   ⚠️ 예상치 못한 응답")
            return []
            
    except requests.exceptions.Timeout:
        print("   ❌ 타임아웃")
        return []
    except Exception as e:
        print(f"   ❌ 오류: {e}")
        return []

def insert_to_db(items):
    """MySQL에 데이터 삽입"""
    if not items:
        return 0
    
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
    except Exception as e:
        print(f"❌ DB 연결 실패: {e}")
        return 0
    
    sql = """
    INSERT INTO lost_item 
    (title, description, category, location, storage_location, found_date, 
     status, external_id, datasource, view_count, received_date, created_at, updated_at)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """
    
    inserted = 0
    skipped = 0
    
    for item in items:
        try:
            external_id = item.get('LOST_MNG_NO', '')
            if not external_id:
                continue
            
            # 중복 체크
            cursor.execute(
                "SELECT COUNT(*) FROM lost_item WHERE external_id = %s AND datasource = 'SEOUL_LOST'",
                (external_id,)
            )
            if cursor.fetchone()[0] > 0:
                skipped += 1
                continue
            
            # 카테고리 매핑
            category_map = {
                '지갑': '지갑', '가방': '가방', '핸드폰': '핸드폰',
                '노트북': '노트북', '의류': '의류', '우산': '우산',
                '서류': '서류', '귀중품': '귀중품', '도서': '도서',
            }
            raw_category = item.get('LOST_KND', '기타')
            category = category_map.get(raw_category, '기타')
            
            # 상태 매핑
            status_map = {
                '보관중': 'STORED', '반환': 'RETURNED', 
                '수령': 'CLAIMED', '습득': 'FOUND',
            }
            raw_status = item.get('LOST_STTS', '보관중')
            status = status_map.get(raw_status, 'STORED')
            
            # 날짜 파싱
            found_date = None
            reg_ymd = item.get('REG_YMD', '')
            if reg_ymd and len(reg_ymd) >= 10:
                found_date = reg_ymd[:10]
            
            received_date = None
            rcv_ymd = item.get('RCV_YMD', '')
            if rcv_ymd and len(rcv_ymd) >= 10:
                received_date = rcv_ymd[:10]
            
            values = (
                item.get('LOST_NM', '무제')[:100],
                item.get('LGS_DTL_CN', ''),
                category,
                item.get('RCPL', ''),
                item.get('CSTD_PLC', ''),
                found_date,
                status,
                external_id,
                'SEOUL_LOST',
                int(item.get('INQ_CNT', 0) or 0),
                received_date,
            )
            
            cursor.execute(sql, values)
            inserted += 1
            
        except Exception as e:
            print(f"   ⚠️ 삽입 실패: {str(e)[:50]}")
            continue
    
    conn.commit()
    cursor.close()
    conn.close()
    
    if skipped > 0:
        print(f"   중복 건너뜀: {skipped}개")
    
    return inserted

def main():
    """메인 실행"""
    print("=" * 60)
    print("🚇 서울시 분실물 데이터 수집")
    print("=" * 60)
    print()
    
    total_collected = 0
    total_inserted = 0
    batch_size = 100
    max_items = 1000  # 최대 1000개까지
    
    for batch_num in range(max_items // batch_size):
        start = batch_num * batch_size + 1
        end = start + batch_size - 1
        
        print(f"\n[{batch_num + 1}] ", end='')
        items = fetch_page(start, end)
        
        if items is None:  # 더 이상 데이터 없음
            print("   더 이상 데이터 없음")
            break
        
        if not items:  # 오류
            print("   오류 발생, 계속 진행")
            continue
        
        total_collected += len(items)
        
        inserted = insert_to_db(items)
        total_inserted += inserted
        print(f"   💾 {inserted}개 저장")
        
        if len(items) < batch_size:
            print("   마지막 배치")
            break
        
        time.sleep(0.5)  # API 부하 방지
    
    print("\n" + "=" * 60)
    print("✅ 수집 완료!")
    print(f"   총 수집: {total_collected}개")
    print(f"   신규 저장: {total_inserted}개")
    print(f"   중복 건너뜀: {total_collected - total_inserted}개")
    print("=" * 60)
    print()
    print("다음 단계:")
    print("1. 백엔드 실행: cd ..\..  &&  mvnw.cmd spring-boot:run")
    print("2. 브라우저: http://localhost:5173")
    print()

if __name__ == "__main__":
    main()

