# 분실물 데이터 수집 스크립트

이 폴더에는 LOST112와 서울교통공사 데이터를 간편하게 수집할 수 있는 스크립트들이 있습니다.

## 📁 스크립트 파일

### 1. `collect_data.ps1` ⭐ 권장
**PowerShell 스크립트 - 가장 기능이 풍부합니다**

**사용법:**
```powershell
cd scripts
.\collect_data.ps1
```

**기능:**
- ✅ 백엔드 서버 상태 자동 확인
- ✅ 수집 옵션 선택 가능 (7일, 30일, 지역별, 전체 등)
- ✅ 수집 진행 상황 실시간 표시
- ✅ 수집 후 자동으로 통계 출력
- ✅ 색상으로 구분된 친절한 안내 메시지

**선택 메뉴:**
1. LOST112 (경찰청 유실물) - 최근 7일
2. LOST112 (경찰청 유실물) - 최근 30일
3. LOST112 (지역별) - 서울/경기/부산/인천
4. 서울교통공사 (지하철 분실물)
5. 전체 수집 (LOST112 + 서울교통공사)

---

### 2. `collect_data_simple.bat`
**배치 파일 - 빠르고 간단합니다**

**사용법:**
- 파일을 **더블클릭**하거나
- 명령 프롬프트에서 실행

**기능:**
- ✅ 클릭 한 번으로 자동 수집 (최근 14일, 서울/경기/부산/인천)
- ✅ LOST112 + 서울교통공사 + 지역 정보 업데이트 자동 실행
- ✅ 가장 빠르고 간편함

---

### 3. `start_backend_and_collect.bat`
**배치 파일 - 백엔드 시작부터 자동화**

**사용법:**
- 파일을 **더블클릭**

**기능:**
- ✅ 백엔드 서버 자동 시작
- ✅ 서버가 준비될 때까지 대기
- ✅ 자동으로 데이터 수집 시작
- ✅ 백엔드가 꺼져있어도 한 번에 실행 가능

---

## 🔄 자동 스케줄러 (백엔드 내장)

백엔드 애플리케이션에 자동 스케줄러가 내장되어 있습니다.

### 활성화 방법
`backend/capstone-backend/src/main/resources/application.yml` 파일에서:
```yaml
scheduler:
  enabled: true  # false를 true로 변경
```

### 스케줄 일정
- **매일 오전 3시**: LOST112 데이터 자동 수집 (최근 7일)
- **매일 오전 4시**: 서울교통공사 데이터 자동 수집
- **매주 일요일 오전 5시**: 전체 지역 정보 재구축
- **30분마다**: 통계 로그 출력

---

## 💡 사용 권장 사항

### 처음 사용하는 경우
1. `start_backend_and_collect.bat` 실행
2. 또는 백엔드를 먼저 실행한 후 `collect_data.ps1` 실행

### 정기적으로 데이터를 수집하려면
- `application.yml`에서 `scheduler.enabled: true` 설정
- 백엔드를 켜두면 자동으로 매일 수집됨

### 빠르게 한 번만 수집하려면
- `collect_data_simple.bat` 더블클릭

---

## ⚠️ 주의사항

1. **백엔드가 실행 중이어야 합니다**
   - `cd backend/capstone-backend`
   - `./mvnw spring-boot:run`

2. **포트 8081이 사용 가능해야 합니다**
   - 백엔드가 8081 포트를 사용합니다

3. **인터넷 연결이 필요합니다**
   - 외부 API에서 데이터를 가져옵니다

4. **API 키가 설정되어 있어야 합니다**
   - `application.yml`에서 LOST112 API 키 확인

---

## 🐛 문제 해결

### "백엔드 서버가 실행되지 않았습니다" 오류
→ 백엔드를 먼저 실행하세요:
```bash
cd backend/capstone-backend
./mvnw spring-boot:run
```

### "원격 서버에 연결할 수 없습니다" 오류
→ 백엔드가 완전히 시작될 때까지 30초 정도 기다린 후 다시 시도하세요

### PowerShell 실행 정책 오류
→ PowerShell을 관리자 권한으로 실행하고:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## 📊 수집 후 확인

수집이 완료되면 다음 URL에서 결과를 확인할 수 있습니다:

- **통계**: http://localhost:8081/api/admin/region-stats
- **분실물 목록**: http://localhost:8081/api/lost-items
- **프론트엔드**: http://localhost:3000 (프론트엔드가 실행 중인 경우)

