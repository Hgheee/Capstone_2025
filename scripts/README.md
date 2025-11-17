# 📁 Scripts 폴더

## 유틸리티 스크립트 모음

### 🔧 **유지보수용 스크립트**

#### `test_api.bat`
- **용도:** API 연결 및 데이터 수집 테스트
- **사용 시기:** 
  - 데이터 수집이 제대로 되는지 확인할 때
  - API 키가 올바르게 설정되었는지 확인할 때
- **실행:**
  ```bash
  .\scripts\test_api.bat
  ```

#### `setup_unique_constraint.bat`
- **용도:** DB에 UNIQUE 제약 조건 추가 (중복 방지)
- **사용 시기:**
  - 최초 설정 시 한 번만 실행
  - 중복 데이터가 계속 저장될 때
- **실행:**
  ```bash
  .\scripts\setup_unique_constraint.bat
  ```

#### `add_unique_constraint.sql`
- **용도:** UNIQUE 제약 조건 추가 SQL 스크립트
- **사용 시기:** 
  - MySQL에서 직접 실행하고 싶을 때
- **실행:**
  ```bash
  mysql -u hogeonhee -p capstone_db
  source scripts/add_unique_constraint.sql;
  ```

---

## ⚠️ 주의사항

### **데이터 수집은 루트의 `collect_data.bat`를 사용하세요!**

```bash
# ✅ 올바른 방법
.\collect_data.bat

# ❌ 잘못된 방법
.\scripts\collect_data.bat  # 존재하지 않음!
```

---

## 🗑️ 정리된 파일들

다음 파일들은 **메인 스크립트로 통합**되어 삭제되었습니다:

- ~~`collect_data.bat`~~ → 루트로 이동
- ~~`collect_data_simple.bat`~~ → 메인 파일로 통합
- ~~`collect_latest.bat`~~ → 메인 파일로 통합
- ~~`complete_reset.bat`~~ → 메인 파일의 옵션 5
- ~~`reset_and_recollect.bat`~~ → 메인 파일로 통합
- ~~`quick_fix_korean.bat`~~ → 더 이상 필요 없음
- ~~각종 README 문서들~~ → 루트 README.md로 통합

---

## 📖 더 많은 정보

- **메인 README:** `../README.md`
- **빠른 설정:** `../SETUP.md`



