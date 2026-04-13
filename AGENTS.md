# SmartPetTodo - Agent Guide

## 프로젝트 목적
- webhook 기반 할 일 앱과 Android 벌칙 시스템을 유지보수합니다.
- 원격 task CRUD와 로컬 벌칙 sidecar 저장을 분리합니다.

## 빠른 시작 명령
- `./gradlew test`
- `./gradlew :app:assembleDebug`
- `./gradlew connectedAndroidTest`

## 기본 작업 순서
1. `README.md`, `AGENTS.md`, `docs/changes/`, `docs/runbooks/`를 읽습니다.
2. `./gradlew test`와 `./gradlew :app:assembleDebug` 기준선을 확인합니다.
3. 원격 task payload와 로컬 penalty sidecar를 섞지 않습니다.
4. UI, penalty engine, Kakao integration, docs/CI를 같이 갱신합니다.

## 완료 조건
- `./gradlew test` 통과
- `./gradlew :app:assembleDebug` 통과
- 관련 docs/README 갱신
- Device Owner / Kakao 전제 조건을 문서에 명시

## 코드 스타일 원칙
- Kotlin 17
- 벌칙 로직은 `penalty/`에 모읍니다.
- Kakao 알림/세션 로직은 `kakao/`에 모읍니다.
- Activity/UI에서 직접 정책 판단하지 말고 selector/enforcer를 경유합니다.

## 민감 경로
- `app/src/main/java/com/smartpet/todo/data/RemoteStorage.kt`: webhook 계약 변경 금지
- `app/src/main/java/com/smartpet/todo/admin/`: Device Owner / lock task
- `app/src/main/java/com/smartpet/todo/kakao/`: 알림 리스너 권한 전제
- `app/src/main/java/com/smartpet/todo/ui/OverdueVerificationActivity.kt`: 사용자가 쉽게 탈출할 수 없게 유지

## 작업 전 체크리스트
- 원격 API URL 확인
- 에뮬레이터/디바이스 상태 확인
- 알림 접근 / Device Owner 전제 조건 확인

## 작업 후 체크리스트
- 테스트/빌드 결과 기록
- README, docs/changes, runbook 반영
- 미해결 리스크 명시

## 절대 금지
- 벌칙 메타데이터를 원격 task payload에 몰래 섞지 말 것
- Device Owner 준비 없이 잠금이 되는 것처럼 말하지 말 것
- 제3자 괴롭힘 자동화를 추가하지 말 것
