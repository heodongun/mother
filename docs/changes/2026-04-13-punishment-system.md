# 2026-04-13: Android Punishment System

## 배경
기존 앱은 overdue 시 경고 화면과 사진 인증만 제공했고, 사용자가 받을 명시적 벌칙과 설정 화면이 없었습니다.

## 목표
- task별 벌칙 선택 시스템 추가
- 자동 추천과 수동 override 지원
- Kakao/전화/lock-task/인증 고정 벌칙 지원
- README, runbook, CI 정비

## 변경 내용
- `penalty/` 패키지 추가: 모델, selector, 로컬 store, manager
- `kakao/` 패키지 추가: notification listener, 세션 캐시, sender
- `admin/` 패키지 추가: DeviceAdminReceiver, lock task controller
- Task editor에 벌칙 선택 UI 추가
- 메인 화면에 벌칙 설정 다이얼로그와 준비 상태 카드 추가
- overdue activity를 non-dismissible proof gate로 재구성
- AndroidManifest에 `CALL_PHONE`, notification listener, device-admin receiver 추가

## 설계 이유
- task 본문은 기존 webhook을 유지하고, 벌칙 메타데이터는 로컬 sidecar로 분리했습니다.
- 코딩/공부처럼 디바이스가 필요한 목표는 자동 selector가 잠금형 벌칙을 피합니다.
- lock task는 공식 Android Device Owner 경로만 허용해 허위 보장을 피했습니다.

## 영향 범위
- Android 앱 전체 UI 흐름
- overdue punishment 처리
- 로컬 설정/상태 관리

## 검증 방법
- `./gradlew test`
- `./gradlew :app:assembleDebug`
- `./gradlew connectedAndroidTest` (온라인 디바이스 필요)

## 남아 있는 한계
- 벌칙 메타데이터는 로컬 전용이라 기기 간 동기화되지 않습니다.
- Kakao 벌칙은 최근 알림 세션이 있어야 즉시 전송됩니다.
- lock task는 Device Owner provisioning이 없으면 실행되지 않습니다.

## 후속 과제
- emulator/manual scenario 자동화
- Kakao 세션 진단 UI 강화
- local sidecar 복원/정리 정책 개선
