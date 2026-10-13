# 릴리즈 규칙 (필독)

**릴리즈 빌드와 배포는 서명 키를 보관한 PC에서만 한다. 다른 PC·다른 IDE·다른 에이전트에서 릴리즈하지 말 것.**

## 서명 키
- 2026-10-14 에 디버그 키 서명을 정식 릴리즈 키로 교체했다. 키 파일 위치: `C:\Users\amuro\.android-keys\lywsd02-release.jks`
- 비밀번호 등은 `~/.gradle/gradle.properties` 의 `LYWSD02_KEYSTORE`, `LYWSD02_KEY_ALIAS`, `LYWSD02_STORE_PASSWORD`, `LYWSD02_KEY_PASSWORD` 에만 있다. **저장소에는 없다.**
- **이 저장소는 공개이므로 키 파일을 저장소에 올리지 말 것.** (`.gitignore` 에 `*.jks`, `*.keystore` 등록됨) 키 파일과 비밀번호는 외부 저장소나 비공개 저장소 등 다른 곳에도 따로 백업할 것
- 키가 없는 PC에서 `assembleRelease` 등 릴리즈 작업을 직접 실행하면 빌드가 중단된다 (실수 방지 장치). 디버그 빌드는 영향 없음
- 키를 잃어버리거나 바꾸면 이미 설치된 앱에 업데이트가 거부된다 (삭제 후 재설치 필요)

## 이전 버전 사용자 안내
- 2026-10-14 이전 릴리즈는 디버그 키로 서명되어 있어, 이 키로 서명한 새 버전은 **업데이트로 설치되지 않는다.** 기존 앱을 삭제하고 새로 설치하도록 릴리즈 노트에 안내할 것 (이 앱은 보존할 DB가 없어 영향 작음)

## 배포
- GitHub 릴리즈(`muro-dot/lywsd02-ble-android-app`) 및 NAS `\\192.168.0.250\web\LYWSD02` (local.properties 의 apk.deploy.dir)

## 금지
- 키 파일·비밀번호를 커밋하지 말 것
- 디버그 키로 릴리즈를 서명하지 말 것
- 같은 versionCode 로 다른 APK 를 다시 배포하지 말 것
