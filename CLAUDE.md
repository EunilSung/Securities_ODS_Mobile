# ODS_Mobile

증권사 ODS 앱. Kotlin Multiplatform(Android + iOS) + Compose Multiplatform + Clean Architecture.
멀티플랫폼 환경·폰/태블릿 화면 구성·코드 구조는 `~/Desktop/Talk` 프로젝트를 그대로 따른다.

---

## 규칙

1. **새 화면·기능은 기존 코드 플로우를 그대로 따른다.** 새 패턴을 임의로 도입하지 말고 같은 계층의 기존 파일
   (`ui/watchlist`, `ui/stockdetail`)을 먼저 보고 그 모양대로 만든다. 모르면 Talk 의 같은 계층을 참고한다.
2. **한글을 하드코딩하지 말고 `Res.string` 으로 뺀다.** 영문은 `composeResources/values`, 한글은 `values-ko`.
3. **`//` 설명 주석은 넣지 않는다.** 함수·클래스 설명은 한글 KDoc(`/** */`)으로 쓴다. 이름은 영어.
4. **기능은 Android·iOS 양쪽에 적용하고 양쪽 컴파일을 통과시킨다.**
   ```bash
   ./gradlew :composeApp:compileDebugKotlinAndroid
   ./gradlew :composeApp:compileKotlinIosSimulatorArm64
   ```
5. **서버 통신은 `data/repository` 의 `*RepositoryImpl` 에서 한다.** ViewModel 은 유스케이스만 쓴다.
6. **서버 응답은 완결된 것만 기기 DB·화면에 반영한다.** 실패하면 가지고 있던 시세를 그대로 둔다.
7. **수정 후에는 폰과 태블릿 레이아웃을 둘 다 에뮬레이터·시뮬레이터에서 확인한다.**
   사용자 실기기는 사용자가 요청했을 때만 쓴다.

## 구조

```
composeApp/src/
  commonMain/kotlin/com/ods/mobile/
    data/      local(DatabaseDriverFactory · repository(로컬 구현) · seed(시연용 데이터)) · mapper · repository(서버 구현 자리)
    domain/    model · repository(interface) · usecase
    ui/        main(폰/태블릿 레이아웃 · 탭) · 도메인별 화면 · uikit(공용 컴포넌트) · theme · util
    di/        Modules.kt (Koin appModule)
    Config.kt  서버 주소 · DB 이름 · 태블릿 기준 폭
  commonMain/sqldelight/          AppDatabase.sq
  commonMain/composeResources/    values(영문) · values-ko(한글) · drawable(벡터 아이콘)
  androidMain/  Android actual + platformModule + MainActivity
  iosMain/      iOS actual + platformModule + MainViewController
iosApp/         Xcode 프로젝트 (빌드 시 :composeApp:embedAndSignAppleFrameworkForXcode 호출)
```

- MVI 변형: 화면 → `XActions` → `XViewModel.onAction` → `StateFlow<XUiState>`, 한 번만 처리할 일은 `XEvent` 를 `Channel` 로
- DI = Koin, 네비게이션 = Voyager, 이미지 = coil3, 통신 = ktor, 기기 DB = SQLDelight
- 화면 함수: `XScreen`(ViewModel 을 받는 진입점) → `XContent`(상태 없는 그리기 함수)
- 등락 색은 국내 관례: 상승 `AppColors.Rise`(빨강), 하락 `AppColors.Fall`(파랑) — `PriceDirection.color` 를 쓴다

## 디자인 토큰 (C안 · 하이브리드)

목록은 간결하게, 현재가·호가·주문은 촘촘하게. 새 화면은 아래 토큰만 쓴다 — 숫자 크기·색을 화면에 직접 적지 않는다.

| 토큰 | 파일 | 내용 |
|---|---|---|
| 색 | `ui/theme/AppColors.kt` | 브랜드 남색, 상승·하락 + 각 바탕색, 라이트·다크 |
| 글자 | `ui/theme/Typography.kt` | `AppTypography` — 숫자 스타일은 고정폭 숫자(`tnum`). 글꼴은 아직 기기 기본 |
| 간격 | `ui/theme/Dimens.kt` | 4의 배수, 화면 좌우 20 |
| 목록 밀도 | `ui/theme/ListDensityTokens.kt` | 기본/촘촘. `LocalListDensity.current.tokens` 로 행 크기를 읽는다. 값은 설정에 저장 |
| 체결 깜빡임 | `ui/uikit/stock/TickFlash.kt` | `Modifier.tickFlash(price)` — 오르면 빨강, 내리면 파랑 바탕을 0.6초. 동작 줄이기면 끔 |

- 전일 대비 문구는 `Stock.changeLabel()`, 색만으로 등락을 알리지 않도록 ▲·▼ 와 부호를 함께 쓴다
- 호가 단위·상하한가는 `domain/util/TickSize.kt`(한국거래소 기준). 가격을 한 단계 올리고 내릴 때는 `tickUp`/`tickDown`

## 현재가 화면

`ui/quote` — 현재가 탭(`QuoteScreen`)과 종목 상세(`ui/stockdetail/StockDetailScreen`)가 같은 `QuoteContent`·`QuoteViewModel` 을 쓴다.

- 머리(`QuoteHeader`) · 봉 차트(`quote/chart/CandleChart` — Canvas, 1분/일/주/월) · 호가 10단(`quote/orderbook/OrderBookView`)
- 화면 폭이 `Config.Layout.QUOTE_SIDE_BY_SIDE_MIN_WIDTH_DP`(640) 이상이면 차트와 호가를 좌우로, 아니면 위아래로
- 현재가 탭은 마지막으로 본 종목을 연다(종목 상세를 열 때마다 기억)
- **로컬 모드의 실시간 시세는 흉내다.** 호가를 보는 동안 `LocalTickSimulator` 가 1~3초마다 한 호가씩 기기 DB 시세를 움직이고,
  호가·봉은 `data/local/seed/DemoMarketData` 가 그 시세로 만든다. 서버가 붙으면 실제 체결 알림이 기기 DB 시세를 갱신하고 시뮬레이터는 끈다

## 폰 / 태블릿

`ui/main/MainScreen.kt` 의 `MainContent` 가 **창의 폭**으로 가른다(`Config.Layout.TABLET_MIN_WIDTH_DP` = 600dp).

- **폰** — 하단 5탭 목록이 바닥에 깔리고, 상세 화면은 오른쪽에서 밀려 들어와 덮는다. 왼쪽→오른쪽으로 밀거나
  Android 뒤로 가기로 닫는다. 폰은 세로 고정.
- **태블릿** — 왼쪽(1)은 탭 목록, 오른쪽(1.4)은 상세. 회전 허용.
- 상세 화면은 둘 다 `LocalRightNavigator` 에 `push`/`replace` 한다. 닫기는 `popToEmptyRoot()`.
  태블릿에서 열린 항목 표시는 `LocalIsTablet` + `rightNavigator.lastItem` 으로 판단한다.

## 실행

```bash
./gradlew :composeApp:installDebug                 # Android (기기가 여러 대면 adb -s 로 지정해 설치)
open iosApp/iosApp.xcodeproj                       # iOS — Xcode 에서 시뮬레이터 선택 후 실행
./gradlew :composeApp:testDebugUnitTest            # 공통 테스트 (Android JVM)
./gradlew :composeApp:iosSimulatorArm64Test        # 공통 테스트 (iOS 시뮬레이터)
```

JDK 17 이 필요하다. `local.properties` 에 `sdk.dir` 을 둔다(gitignore 대상).

## 주문

`ui/order` — 주문 탭(`OrderScreen`), 현재가에서 연 주문 화면(`OrderTicketScreen`), 태블릿 가로의 주문 칸(`OrderPane`)이
같은 `OrderContent`·`OrderViewModel` 을 쓴다.

- 패널(`order/panel/OrderPanel`): 매수·매도, 지정가·시장가, 가격(± 한 호가), 수량(10·25·50%·최대), 주문가능·주문금액, 검사 결과
- 확인 화면(`order/confirm/OrderConfirmDialog`): 확인 → 보내는 중 → 접수·거부·확인 불가. **확인 불가(`OrderResult.Unreachable`)는
  접수됐을 수 있으므로 다시 보내라고 하지 않고 미체결을 먼저 보게 한다.** 인증(비밀번호·생체)은 아직 없다 — 생기면 확인과 보내기 사이에 넣는다
- 검사 규칙은 `domain/util/OrderValidation.kt`(`checkOrder`): 호가 단위·상하한가·주문가능 금액·매도가능 수량은 오류, 현재가와 5% 이상 차이는 경고.
  시장가 매수는 상한가로 금액을 묶는다. 접수하는 저장소가 금액·수량을 다시 확인한다
- 현재가 → 주문은 `rememberOrderLauncher()` 한 곳에서 가른다. 주문 칸이 있으면(`LocalOrderPaneVisible`) 거기에 넣고, 없으면 `OrderTicketScreen` 을 쌓는다.
  호가 행을 누르면 그 가격으로 주문한다
- 폰: 상세 위에 주문이 쌓이면 뒤로 가기·상단 뒤로는 한 단계(`popOrClose`), 왼쪽에서 밀면 모두 닫는다
- 로컬 모드: 예수금·보유 종목은 `data/local/seed/AccountSeed`, 접수한 주문은 `OpenOrderEntity` 에 미체결로 남는다. 체결은 흉내 내지 않는다

## 미체결 · 정정 · 취소

`ui/openorder` — 주문 탭 위쪽 [주문 | 미체결 N] 에서 연다(`OrderScreen` 이 두 구역을 가른다). 하단 주문 탭 배지는 미체결 건수다.

- 정정은 거래소 방식대로 **남은 수량 전체의 가격만** 바꾸고 새 주문번호를 받는다(`Order.originalOrderNo` 에 원주문번호).
  수량 줄이기·일부 취소는 아직 없다. 시장가 주문은 정정할 수 없다
- 검사는 `checkAmend`: 호가 단위·상하한가·같은 가격은 오류, 매수 정정은 **올린 만큼(`amendExtraAmount`)만** 주문가능 금액과 비교한다
- 취소는 남은 수량 전체. 결과는 `OrderChangeResult` — 받는 쪽에 이미 없는 주문은 `NOT_OPEN` 으로 거부, 응답이 없으면 `Unreachable`
- 로컬 모드 주문번호는 `OrderNumberEntity` 순번에서 받는다 — 취소한 번호를 다시 쓰지 않는다
- 저장소 테스트(`LocalOrderRepositoryTest`)는 인메모리 DB(`testsupport/createTestDatabase`)로 Android·iOS 양쪽에서 돈다

## 잔고

`ui/balance` — 위에 총자산·평가손익·수익률과 예수금·주문가능·매입·평가금액, 아래에 보유 종목. 종목을 누르면 현재가 상세를 연다.

- 계산은 `domain/model/Balance.kt`: `Holding`(평가금액·손익·수익률·매도가능), `BalanceSummary`(총자산 = 예수금 + 평가금액).
  **수수료·세금을 빼지 않은 단순 손익**이다. 미체결 매수는 총자산을 바꾸지 않고 주문가능만 줄인다
- 보유 종목은 `selectHoldings` 한 쿼리로 보유 + 시세 + 미체결 매도를 묶는다. 관심종목에 없는 보유 종목은 이름 대신 코드, 시세 대신 평균단가
- 금액 가리기: **금액·수량만** 가리고(`masked`), 수익률·현재가는 공개 시세라 둔다. 선택은 `UserPreferenceRepository.hideAmounts` 에 저장돼
  다음 실행에도 유지된다. 지금은 잔고 탭에만 적용된다
- 손익 색도 시세와 같은 관례(이익 빨강·손실 파랑)

## 보안

### 주문 인증 (`ui/orderauth`)

주문·정정·취소는 **확인 창 → 인증 → 보내기** 순서다. 확인 창의 보내기 버튼은 `awaitingAuth` 만 켜고, 인증 창이
`OnAuthorized` 를 보내야 실제로 보낸다(`OrderViewModel.submit`, `OpenOrdersViewModel.submit` 은 인증 뒤에만 불린다).

- 국내 증권사처럼 **4자리 주문(계좌) 비밀번호** + 선택적 생체 인증. 첫 주문 때 등록하고, 같은 숫자·연속 숫자는 막는다(`OrderPinPolicy`)
- 비밀번호는 salt + SHA-256 해시로만 저장(`PinHash`, krypto). 5번 틀리면 10분 잠금 — 로컬 모드에서는 기기가 세고, 서버가 붙으면 서버가 센다
- 생체: Android `BiometricPrompt`(BIOMETRIC_STRONG 만, 그래서 `MainActivity` 는 `FragmentActivity`), iOS `LAContext`(기기 암호 대체 없음).
  취소·실패·불가면 비밀번호로 넘어간다. iOS 는 `Info.plist` 의 `NSFaceIDUsageDescription` 이 없으면 종료된다
- 키패드는 **열 때마다·틀릴 때마다 숫자 배열을 섞는다**(`SecurePinPad`)
- 인증 창 ViewModel 은 주문·미체결 화면이 하나를 같이 쓴다(`order-auth` 키). 열 때마다 `OnStart` 로 처음부터 시작한다
- 인증 창은 목적(`OrderAuthPurpose`)이 셋이다 — `AUTHORIZE`(주문: 생체 먼저), `VERIFY_PIN`(설정 전 본인 확인: 비밀번호만),
  `CHANGE_PIN`(현재 → 새 비밀번호 두 번, 지금과 같은 비밀번호 거부). 비밀번호 변경·본인 확인은 생체 설정을 건드리지 않는다

### 앱 전환기 화면 가리기

Talk 구조를 그대로 옮겼다(`Config.AppSwitcherSnapshot`).

- Android: `AppSwitcherSnapshotGuard` — 13 이상은 최근 앱 스냅샷 끄기, 일시정지 동안은 `FLAG_SECURE`. 훅은 `MainActivity` 에만
- iOS: `IosAppSwitcherCover.kt` — 비활성이 되는 순간 UIWindow 맨 위에 빈 화면 같은 가림막(차트 아이콘)을 올리고 활성이 되면 내린다.
  Compose 로 그리면 백그라운드 진입 뒤 그리기가 멈춰 스냅샷을 못 가린다
- Android 최근 앱 화면에 들어가는 순간 맨 위 앱은 실제 화면(라이브 타일)으로 보인다. 저장되는 썸네일이 가려지는지는 홈으로 나갔다 최근 앱을 열어 확인한다

## 로그인

`ui/login`(화면) · `LoginRepository`(로컬 구현 `LocalLoginRepositoryImpl`). Talk 처럼 `MainScreen` 이 로그인 여부로 갈라 그린다 —
로그인 전에는 로그인 화면**만** 그리고 탭 화면을 아래에 깔지 않는다(가려진 화면이 눌리거나 읽히지 않게).

- 로컬 모드는 시연용 계정(`DemoAccounts`: test1 ~ test3, 비밀번호 1234)으로만 들어간다. 서버가 붙으면 `Config.Server.IS_ENABLED` 로 서버 구현과 갈라 끼운다
- Talk 와 달리 **비밀번호를 저장하지 않는다**(자동 로그인 없음). 기억하는 것은 아이디뿐이고 `아이디 저장` 을 끄면 지운다
- 같은 아이디로 5번 틀리면 5분 잠금(`LoginPolicy`). 없는 아이디도 똑같이 세고 같은 문구를 보인다 — 어느 아이디가 있는지 드러내지 않는다
- 서버에 닿지 못하면(예외) `Unreachable` — 비밀번호를 비우지 않는다. 틀림·잠김이면 비밀번호를 비운다
- 로그아웃은 더보기 맨 위 고객 카드(이름 · 가린 계좌번호 `maskAccountNo` · 로그아웃). `MainViewModel` 이 `LoggedOut` 을 보내면 오른쪽 스택을 비우고,
  탭 화면은 구성에서 빠지므로 다음 로그인은 첫 탭부터 새로 시작한다
- Android 에서 `adb shell input keyevent 66`(하드웨어 Enter)으로 완료를 누르면 Compose 가 첫 입력칸에 초점을 되돌린다. 실제 소프트 키보드의 완료 버튼은 그렇지 않다

## 더보기

`ui/more/MoreScreen`(목록) → `ui/setting/detail/*`(상세, 오른쪽 스택). Talk 의 설정 구조와 같다 — 상세는 `popToEmptyRoot()` 뒤 `push`.
상세 화면들은 `SettingViewModel` 하나를 같이 쓰고, 틀은 `SettingDetailScaffold`(인증 창도 여기서 띄운다).

- 주문 인증: 비밀번호 상태·변경(없으면 등록), 생체 켜기·끄기. **켤 때만** 비밀번호로 본인 확인, 끄기는 바로. 기기에 생체가 없으면 스위치를 막고 이유를 보인다
- 맨 위: 로그인한 고객 카드와 로그아웃(위 `로그인` 참고)
- 화면: 목록 밀도, 잔고 금액 가리기 — 앱 전체 값이라 바로 반영된다
- 앱 정보: 버전(`AppVersionProvider`, 플랫폼별), 시세·계좌가 시연용인지(`Config.Server.IS_ENABLED`)

## 화면 폭 기준 (`Config.Layout`)

| 폭 | 레이아웃 |
|---|---|
| 600dp 미만 | 폰 — 탭 목록 + 덮개 |
| 600dp 이상 | 태블릿 2칸 — 목록 · 상세 |
| 840dp 이상 | 태블릿 3칸 — 목록 · 차트+호가 · 주문 |

## 아직 없는 것

서버 연동·푸시·화면잠금·보안 SDK 는 아직 넣지 않았다. Talk 에 같은 기능이 있으면 그 구조를 옮겨 온다.
체결, 일부 정정·취소, 서버 로그인(자동 로그인·세션 만료 포함), 서버 연동, 전용 글꼴(Pretendard)은 아직 없다.
