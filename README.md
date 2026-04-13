# 마더 Android

`마더`는 마감이 지난 할 일을 단순 알림으로 끝내지 않고, 사용자가 미이행 비용을 직접 설정해 목표 달성을 돕는 Android 앱입니다.

## 핵심 기능

- webhook 기반 할 일 CRUD
- task별 벌칙 선택: 자동 추천 / 수동 지정
- 벌칙 종류
  - 인증 고정
  - 앱 고정 잠금(lock task, 공식 kiosk 모드가 준비된 경우만)
  - 책임 파트너 전화
  - 책임 파트너 KakaoTalk 메시지
- overdue 시 punishment trigger + 사진 인증 gate
- Kakao 알림 세션 캐시 기반 책임 메시지 전송

## 기술 스택

- Kotlin
- Android SDK 34
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL

## 요구 환경

- Android Studio 또는 JDK 17 + Android SDK
- `./gradlew` 실행 가능 환경
- 선택 사항
  - `MOTHER_TASKS_BASE_URL`
  - `MOTHER_VERIFY_URL`

## 설치 및 실행

```bash
./gradlew :app:assembleDebug
```

생성 APK:

- `app/build/outputs/apk/debug/app-debug.apk`

## 테스트

```bash
./gradlew test
./gradlew :app:assembleDebug
./gradlew connectedAndroidTest
```

`connectedAndroidTest`는 온라인 에뮬레이터/디바이스가 필요합니다.

## 환경 변수 / .env

`.env.example` 예시:

```env
MOTHER_TASKS_BASE_URL=https://heodongun.com/webhook/mother/tasks
MOTHER_VERIFY_URL=https://heodongun.com/webhook/mother/verify-photo
```

## 벌칙 설정 요약

- 연락형 벌칙은 동의한 책임 파트너에게만 사용합니다.
- Kakao 벌칙은 앱의 알림 접근 권한과 최근 알림 세션이 필요합니다.
- 앱 고정 잠금은 Device Owner + lock task allowlist가 준비된 경우에만 동작합니다.
- 코딩/공부처럼 디바이스가 필요한 목표는 자동으로 잠금형 벌칙을 피합니다.

## 빠른 개발 명령

```bash
./gradlew test
./gradlew :app:assembleDebug
./gradlew connectedAndroidTest
```

## 폴더 구조

```text
app/src/main/java/com/smartpet/todo/
  admin/      # device admin, lock task
  kakao/      # Kakao notification listener/session sender
  penalty/    # local sidecar store, selector, enforcer
  ui/         # Compose screen/activity
  viewmodel/  # Task + penalty 상태 조합
```

## 알려진 제한 사항

- 원격 webhook은 task 본문만 저장하고, 벌칙 메타데이터는 기기 로컬에만 저장됩니다.
- Kakao 벌칙은 최근에 알림을 받은 방 세션이 있어야 바로 전송됩니다.
- Device Owner provisioning 없이 앱 잠금 벌칙은 준비 상태만 표시되고 실행되지 않습니다.

## 문서

- 변경 기록: `docs/changes/`
- lock task 준비: `docs/runbooks/android-device-owner-lock-task.md`
