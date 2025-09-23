import os
from dotenv import load_dotenv
import mysql.connector

load_dotenv()

try:
    conn = mysql.connector.connect(
        host=os.getenv('DB_HOST'),
        port=int(os.getenv('DB_PORT')),
        user=os.getenv('DB_USER'),
        password=os.getenv('DB_PASSWORD'),
        database=os.getenv('DB_NAME')
    )
    print('DB 연결 성공!')
    cursor = conn.cursor()
    cursor.execute('SELECT COUNT(*) FROM lost_items_temp')
    count = cursor.fetchone()[0]
    print(f'lost_items_temp 테이블에 {count}개 레코드가 있습니다.')
    conn.close()
except Exception as e:
    print(f'DB 연결 실패: {e}')
