# ODS — 증권사 아웃도어 세일즈 앱

영업 직원이 태블릿을 들고 지점 밖에서 고객을 만나 **방문 일정 → 상담 → 투자성향 진단 → 상품 가입 / 계좌 개설**을 현장에서 마치는 앱.
Kotlin Multiplatform(Android · iOS), Compose Multiplatform, Clean Architecture + MVI. 지금은 서버 없이 기기 안 시연용 데이터로 돈다.

## 기능

| 영역 | 내용 |
|---|---|
| 직원 인증·단말 보안 | 사번 로그인, 직원별 6자리 앱 잠금(+생체), 5분 무조작·백그라운드 1분 초과 자동 잠금, 5회 오류 시 업무 데이터 삭제, 앱 전환기 가리기, 배포 빌드 화면 캡처 차단, 고객 개인정보 가리기 |
| 방문 일정 | 주간 달력, 일정 등록(고객·30분 단위 시각·장소·목적), 방문 완료 → 목적에 맞는 상담 기록 |
| 고객 | 이름·초성·전화번호 검색, 상세(투자성향·계좌·가입 상품·방문 예정·상담 이력), 신규 등록, 상담 기록 |
| 투자성향 진단 | 7문항, 5단계 성향, 1년 유효기간, 고령투자자 안내, 고객 확인·전자서명 |
| 상품 | 위험등급(1~6), 핵심 설명, 적합성 원칙, 가입 절차(고객 → 적합성 → 설명 확인 → 계좌·금액 → 서명), 고령투자자 숙려기간 |
| 계좌 개설 | 신분증 촬영(카메라), 신분증-고객 정보 대조·진위 확인, 계좌 종류 제한(ISA 등), 약관 동의, 고객 직접 입력 비밀번호, 서명 |
| 시장 | 관심종목 시세·차트·호가, **고객에게 크게 보여 주기**(전체 화면) |
| 태블릿 | 2칸(목록·상세), 가로 3칸(목록·상세·상담 칸) |

시연용 계정: `test1` ~ `test3` / `1234` → 처음 로그인하면 앱 잠금 비밀번호 6자리를 정한다.

## 실행

```bash
./gradlew :composeApp:installDebug          # Android
open iosApp/iosApp.xcodeproj                # iOS (Xcode 에서 실행)
```

JDK 17, `local.properties` 의 `sdk.dir` 이 필요하다. 구조·규칙은 [CLAUDE.md](CLAUDE.md) 에 있다.

## 검증 — 한 번에

서버 없는 로컬 모드 앱이라 서버를 띄우지 않고 앱 테스트와 실화면 흐름만 돈다.

```bash
./gradlew :composeApp:verifyAll --continue
```

Android Studio 에서는 실행 구성 **`검증 - 전체 (실화면 포함)`** 을 고르고 실행한다(고칠 때마다는 `검증 - 빠르게 (JVM)`).

1. 단위·ViewModel 시나리오 테스트(Android JVM) — 업무 규칙과 화면 상태·이벤트를 인메모리 DB 위에서 본다
2. 같은 테스트(iOS 시뮬레이터)
3. Android 앱 빌드 → 에뮬레이터에 설치 → Maestro 실화면 흐름 7개
4. iOS 앱 빌드 → 시뮬레이터에 설치 → 같은 흐름 7개

**준비** — Maestro(`curl -fsSL https://get.maestro.mobile.dev | bash`), Xcode, 그리고 `local.properties` 에 이 PC 의 검증 기기:

```properties
e2e.androidAvd=<에뮬레이터 이름>     # 꺼져 있으면 켠다
e2e.iosDevice=<시뮬레이터 UDID 또는 이름>
```

**결과** — `composeApp/build/test-results/`(JVM·iOS 테스트), `build/maestro/<android|ios>/report.xml` 과 스크린샷·로그.

**자동으로 확인하지 못하는 것** — 실제 카메라 촬영, 생체 인증, 배포 빌드의 화면 캡처 막기. 손으로 확인한다.
