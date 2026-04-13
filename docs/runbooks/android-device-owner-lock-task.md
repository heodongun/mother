# Android Device Owner / Lock Task 준비

## 왜 필요한가
`앱 고정 잠금` 벌칙은 Android 공식 lock task mode를 사용합니다. 이 모드는 Device Owner 또는 allowlist 설정이 없는 일반 앱에서는 진짜로 강제되지 않습니다.

## 전제 조건
- 초기화된 테스트 디바이스 또는 전용 기기
- ADB 연결
- 앱이 설치되어 있어야 함

## 기본 명령

```bash
adb shell dpm set-device-owner com.smartpet.todo/com.smartpet.todo.admin.MotherDeviceAdminReceiver
```

## 확인 방법
- 앱 설정 > 벌칙 설정에서
  - `기기 소유자 연결됨`
  - `lock task 가능`
  상태가 모두 보이면 준비 완료입니다.

## 실패 원인
- 이미 계정이 연결된 일반 사용자 디바이스
- 기존 Device Owner가 남아 있는 디바이스
- receiver/component 이름 오타

## 해제 / 재설정 주의
Device Owner는 기기 정책에 큰 영향을 줍니다. 일반 사용자 기기에는 적용하지 말고, 테스트용 디바이스에서만 사용하세요.
