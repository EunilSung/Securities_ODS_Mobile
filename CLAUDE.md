# ODS_M

증권사 **ODS(아웃도어 세일즈)** 앱. 영업 직원이 태블릿을 들고 지점 밖에서 고객을 만나 계좌 개설·상품 가입·상담을 현장에서 마친다.
Kotlin Multiplatform(Android + iOS) + Compose Multiplatform + Clean Architecture(MVI 변형).
멀티플랫폼 환경·폰/태블릿 화면 구성·코드 구조는 `~/Desktop/Talk` 를 따르고, 시세·차트 부품은 `~/Desktop/ODS_Mobile` 에서 가져왔다.
서버는 아직 없다 — 기기 안 시연용 데이터로 도는 **로컬 모드**만 있고, 서버 구현을 끼울 자리(`Config.Server`, `di/Modules.kt`)를 남겨 두었다.

---

## 규칙

1. **새 화면·기능은 기존 코드 플로우를 그대로 따른다.** 새 패턴을 임의로 도입하지 말고 같은 계층의 기존 파일을 먼저 본다
   (목록·상세는 `ui/customer`, 단계별 절차는 `ui/product/subscribe`·`ui/account`).
2. **한글을 하드코딩하지 말고 `Res.string` 으로 뺀다.** 영문은 `composeResources/values`, 한글은 `values-ko`.
   상담 이력에 자동으로 남기는 문구도 화면이 `stringResource` 로 만들어 액션에 실어 보낸다(유스케이스는 문구를 만들지 않는다).
   상품 설명·시연용 고객처럼 **데이터**는 예외다(서버가 내려 주는 값).
3. **`//` 설명 주석은 넣지 않는다.** 함수·클래스 설명은 한글 KDoc(`/** */`)으로 쓴다. 이름은 영어.
4. **기능은 Android·iOS 양쪽에 적용하고 양쪽 컴파일을 통과시킨다.**
   ```bash
   ./gradlew :composeApp:compileDebugKotlinAndroid
   ./gradlew :composeApp:compileKotlinIosSimulatorArm64
   ```
5. **ViewModel 은 유스케이스만 쓴다.** 저장소·서버를 직접 부르지 않는다. 여러 저장소를 잇는 업무(가입·계좌 개설·진단 저장)는 유스케이스가 하고,
   화면이 이미 막은 조건도 유스케이스가 **한 번 더 확인**한다(적합성·금액·계좌·서명·약관).
6. **서버 응답은 완결된 것만 기기 DB·화면에 반영한다.** 실패하면 가지고 있던 것을 그대로 둔다.
7. **고객 개인정보는 기기에 최소한만 남긴다.** 신분증 사진·주민번호 뒷자리·계좌 비밀번호·서명 그림은 저장하지 않는다(서명은 지문 해시만).
   화면에 보이는 전화번호·생년월일·주소·계좌번호는 `maskPersonalInfo` 설정을 따른다.
8. **수정 후에는 단위 테스트와 Maestro 실화면 흐름을 Android·iOS 에서 돌린다.** 폰·태블릿 레이아웃을 둘 다 본다. 사용자 실기기는 요청했을 때만 쓴다.
9. **기능마다 브랜치를 따서 진행하고, 검증이 끝나면 `main` 에 머지한다.**

## 구조

```
composeApp/src/
  commonMain/kotlin/com/eunilsung/ods/
    data/      local(DatabaseDriverFactory · LocalDataWiperImpl · repository(로컬 구현) · seed(시연용 데이터)) · mapper
    domain/    model · repository(interface) · usecase · util(업무 규칙)
    ui/        main(폰/태블릿 레이아웃 · 탭) · 도메인별 화면 · uikit(공용 부품) · theme · util
    di/        Modules.kt (Koin appModule)
    Config.kt  서버 주소 · DB 이름 · 화면 폭 기준 · 앱 전환기 가리기 · 화면 캡처 막기
  commonMain/sqldelight/          AppDatabase.sq
  commonMain/composeResources/    values(영문) · values-ko(한글) · drawable(벡터 아이콘)
  androidMain/  actual · platformModule · MainActivity · AppSwitcherSnapshotGuard · 카메라(FileProvider)
  iosMain/      actual · platformModule · MainViewController · IosAppSwitcherCover · 카메라(UIImagePickerController)
iosApp/         Xcode 프로젝트 (Info.plist 에 Face ID·카메라·사진첩 사용 설명)
.maestro/       실화면 흐름(Android·iOS 공용)
```

- MVI 변형: 화면 → `XActions` → `XViewModel.onAction` → `StateFlow<XUiState>`, 한 번만 처리할 일은 `XEvent` 를 `Channel` 로
- 화면 함수: `XScreen`(Voyager Screen, ViewModel 을 받는 진입점) → `XContent`(상태 없는 그리기 함수)
- 오른쪽 스택에 올리는 Screen 의 인자는 **직렬화되는 값만**(String·enum). Android 가 화면 상태를 저장할 때 직렬화한다(날짜는 ISO 문자열로)
- DI = Koin, 네비게이션 = Voyager, 통신 = ktor(자리만), 기기 DB = SQLDelight, 설정 = multiplatform-settings, 해시 = krypto

## 탭과 화면

| 탭 | 목록(왼쪽) | 상세(오른쪽 스택) |
|---|---|---|
| 일정 | `ui/schedule` 주간 달력 + 고른 날 방문 | 방문 상세 → 완료 시 상담 기록 / 일정 등록 |
| 고객 | `ui/customer/list` 이름·초성·전화번호 검색 | 고객 상세 → 상담 기록 · 투자성향 진단 · 계좌 개설 · 방문 일정 |
| 상품 | `ui/product/list` 종류별 | 상품 상세(핵심 설명·적합성) → 가입 절차 |
| 시장 | `ui/watchlist` 관심종목 시세 | 현재가(차트·호가) → 고객에게 보여 주기(전체 화면) |
| 더보기 | `ui/more` 직원·로그아웃 | 보안 · 화면 · 앱 정보 |

- **상담 중 고객**(`ConsultSessionRepository`): 고객 상세·가입·계좌 개설·방문 완료가 그 고객과 상담을 시작한다. 로그아웃·데이터 삭제 때 비운다
- 업무를 마치면 상담 이력에 자동 기록이 남는다(진단·가입·개설). 기록한 직원은 로그인한 직원이다

## 폰 / 태블릿 (`Config.Layout`)

| 창 폭 | 레이아웃 |
|---|---|
| 600dp 미만 | 폰 — 탭 목록 + 상세가 오른쪽에서 밀려 덮는다(세로 고정) |
| 600dp 이상 | 태블릿 2칸 — 목록 · 상세 |
| 1000dp 이상 | 태블릿 3칸 — 목록 · 상세 · **상담 칸**(`ui/consult/ConsultPane`: 상담 중 고객 요약 + 바로 기록 + 최근 상담) |

기준은 기기 종류가 아니라 지금 창의 폭이다. 상세 화면은 모두 `LocalRightNavigator` 에 쌓는다.

## 업무 규칙 (`domain/util`)

- **직원 인증·단말 잠금**(`AppLockPolicy`): 로그인 후 직원별 6자리 앱 잠금 비밀번호(+생체). 5분 무조작·백그라운드 1분 초과 시 잠금.
  **5회 틀리면 업무 데이터(`wipeBusinessData`)와 모든 잠금 비밀번호를 지우고 로그아웃**(단말 분실 대비). 잠겨도 업무 화면은 버리지 않는다
- 로그인(`LoginPolicy`): 사번 5회 오류 시 5분 잠금, 비밀번호 저장 안 함
- 투자성향(`RiskAssessmentPolicy`): 7문항 → 0~100 → 5단계. 유효기간 1년(`hasValidRiskProfile`). 65세 이상 고령투자자 안내
- 적합성(`suitabilityFor`): 성향 없음·만료 → 진단부터, 성향보다 위험한 상품 → 고객 자발 확인 없이는 진행 불가.
  고령투자자의 고난도·1~2등급 상품 → 2일 숙려기간(`SubscriptionPolicy`)
- 계좌 개설(`AccountOpeningPolicy`): 신분증 이름·생년월일·성별 숫자 대조(`mismatchesWith`) → 진위 확인. ISA 만 19세·1인 1계좌, 연금저축 중복 금지.
  계좌 비밀번호 4자리(같은·연속·생년월일 숫자 금지), 고객이 직접 입력하고 저장하지 않음
- 서명(`SignaturePolicy`): 점 10개 이상, 칸의 15% 이상 퍼져야 받는다

## 보안

- 앱 전환기 가리기: Android `AppSwitcherSnapshotGuard`, iOS `IosAppSwitcherCover`(Talk 구조)
- 화면 캡처 막기: **배포용 Android 빌드만** `FLAG_SECURE`(`Config.Security.BLOCK_SCREEN_CAPTURE_IN_RELEASE`). 디버그는 스크린샷·Maestro 검증을 위해 막지 않는다. iOS 는 막는 공개 API 가 없다
- 비밀번호류는 salt + SHA-256(`PinHash`)
- 신분증 사진: Android 는 카메라 앱이 캐시 임시 파일에 쓰고 앱이 읽자마자 지운다. iOS 는 메모리로만 받는다. 개설을 마치면 상태에서도 버린다

## 테스트와 검증

```bash
./gradlew :composeApp:verifyAll --continue         # 전체: JVM 테스트 → iOS 시뮬레이터 테스트 → 실화면(Android → iOS)
./gradlew :composeApp:testDebugUnitTest            # 공통 테스트 (Android JVM)
./gradlew :composeApp:iosSimulatorArm64Test        # 공통 테스트 (iOS 시뮬레이터)
./gradlew :composeApp:e2eAndroid                   # 실화면 흐름만 (Android) — 빌드·설치까지 한다
./gradlew :composeApp:e2eIos                       # 실화면 흐름만 (iOS)
```

- **Android Studio 에서는 실행 구성을 고른다**(`.run`): 고칠 때마다 `검증 - 빠르게 (JVM)`, 머지 전에는 `검증 - 전체 (실화면 포함)`.
  전체는 기기를 켜는 것까지 18분쯤 걸린다(Android 태블릿 에뮬레이터가 가장 느리다)
- **기기는 `local.properties` 의 `e2e.androidAvd`(에뮬레이터 이름)·`e2e.iosDevice`(시뮬레이터 UDID·이름)로 고른다.** 꺼져 있으면 켠다.
  없으면 켜져 있는 에뮬레이터·iPhone 시뮬레이터 중 첫 번째를 쓰고, 실기기는 고르지 않는다. iOS 테스트(`iosSimulatorArm64Test`)도 같은 시뮬레이터에서 돈다
- 기기 고르기·켜기·설치·Maestro 실행은 `.maestro/run.sh` 가 한다(`.maestro/run.sh android <apk>`, `.maestro/run.sh ios`). 서버는 띄우지 않는다
- 결과: JVM `composeApp/build/test-results/testDebugUnitTest`, iOS `…/iosSimulatorArm64Test`, 실화면 `build/maestro/<플랫폼>/report.xml` 과 같은 폴더의 스크린샷·로그
- `--continue` 로 돌리면 하나가 깨져도 끝까지 돌고 깨진 단계가 마지막에 모여 나온다

- 저장소·ViewModel 테스트는 인메모리 DB(`testsupport/createTestDatabase`). SQLDelight 흐름은 `Dispatchers.Default` 에서 오므로
  `advanceUntilIdle` 대신 `uiState.first { 조건 }` 으로 기다린다
- Maestro 요소는 `ui/uikit/TestTags.kt` 의 이름을 `id` 로 찾는다. 다이얼로그는 Surface 에 `exposeTestTags()` 를 붙인다.
  태블릿에서 두 칸에 같은 부품이 뜨면(주간 달력) 태그 접두어로 가른다
- 흐름: 로그인·시장·고객에게 보여 주기 / 앱 잠금·데이터 삭제 / 고객·상담·신규 등록 / 방문 일정 / 투자성향 진단·서명 / 상품 가입 / 계좌 개설
- **Maestro 주의**
  - 키보드 내리기는 `pressKey: Enter` 로 한다(`common/dismiss_keyboard.yaml`). Android `hideKeyboard` 는 뒤로 가기라 키보드가 없으면 앱이 닫힌다
  - 에뮬레이터 키보드가 한글 자판이면 영문 입력이 한글로 바뀐다 — `run.sh` 가 흐름 전에 영문(ASCII) 자판이 될 때까지 언어 전환 키(204)를 누른다
  - iOS 에는 시스템 뒤로 가기가 없어 상단 뒤로(`top_back`)를 누른다. 폰에서 상세가 닫히는 애니메이션 중에는 탭이 먹지 않으니 `waitForAnimationToEnd`
  - 서명은 `common/sign.yaml` 이 서명 칸에서 두 번 swipe 한다
  - 신분증은 시연용 신분증 버튼으로 넘긴다(로컬 모드에서만 보인다). 실제 카메라는 손으로 확인한다
- 자동으로 확인하지 못하는 것: 실제 카메라 촬영(iOS 시뮬레이터는 카메라 화면만 뜨고 찍히지 않는다), 생체 인증, 배포 빌드의 캡처 막기, 앱 잠금의 시간 경과(단위 테스트로 본다)

## 아직 없는 것

서버(로그인·고객·일정·상품·가입·계좌 개설·진위 확인·원격 단말 잠금), 신분증 OCR·사진 속 주민번호 가리기, 숙려기간 중 철회,
일부 해지·가입 내역 상세, iOS `Info.plist` 문구의 영문화, 전용 글꼴.
