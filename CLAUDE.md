# Securities_ODS_Mobile

증권사 ODS(아웃도어 세일즈) 앱. 영업 직원이 태블릿으로 지점 밖에서 고객을 만나 상담·투자성향 진단·상품 가입·계좌 개설을 마친다.
Kotlin Multiplatform(Android + iOS), Compose Multiplatform, Clean Architecture + MVI.
서버 주소(`server.baseUrl`)가 있으면 **Ktor 서버(`:server`)에 붙는 서버 모드**, 비어 있으면 **기기 안 시연용 데이터로만 도는 로컬 모드**다.
앱과 서버의 요청·응답 모델은 `:shared` 에 있다.
기능·화면 설명은 [README.md](README.md), 디자인 방향은 `docs/design-proposal.html` 에 있다.

## 규칙

1. **새 화면·기능은 기존 코드 플로우를 따른다.** 같은 계층의 기존 파일을 먼저 보고 그 모양대로 만든다
   (목록·상세는 `ui/customer`, 단계별 절차는 `ui/product/subscribe`·`ui/account`). 새 패턴을 임의로 들이지 않는다.
2. **한글을 하드코딩하지 않는다.** `Res.string` 으로 빼고 영문은 `composeResources/values`, 한글은 `values-ko` 에 둔다.
   상담 이력에 자동으로 남기는 문구도 화면이 `stringResource` 로 만들어 액션에 실어 보낸다. 상품 설명·시연용 고객 같은 **데이터**는 예외다.
3. **`//` 설명 주석은 넣지 않는다.** 함수·클래스 설명은 한글 KDoc(`/** */`), 이름은 영어.
4. **Android·iOS 양쪽에 적용하고 양쪽 컴파일을 통과시킨다.** 한쪽만 되는 기능이면 먼저 알린다.
5. **화면은 ViewModel 만, ViewModel 은 유스케이스만 본다.** 여러 저장소를 잇는 업무는 유스케이스가 하고,
   화면이 이미 막은 조건(적합성·금액·계좌·서명·약관)도 저장 직전에 유스케이스가 **한 번 더 확인**한다.
6. **고객 개인정보는 기기에 최소한만 남긴다.** 신분증 사진·주민번호 뒷자리·계좌 비밀번호·서명 그림은 저장하지 않는다(서명은 해시만).
   화면의 전화번호·생년월일·주소·계좌번호는 `maskPersonalInfo` 설정을 따른다.
7. **고친 뒤에는 `verifyAll` 을 통과시킨다** — 서버 테스트, 단위·ViewModel 시나리오·실 서버 기능 테스트(JVM·iOS)와 Maestro 실화면 흐름(Android·iOS, 서버 모드).
   사용자 실기기는 요청했을 때만 쓴다.
8. **기능마다 브랜치를 따고, 7번이 끝나면 `main` 에 머지한다.** 원격 저장소가 없으니 푸시는 하지 않는다.

### 서버

- 스키마는 `server/src/main/resources/db/migration` 의 Flyway SQL 이 기준이다. 나간 마이그레이션은 고치지 않고 새 번호를 더한다.
- 누구인지는 토큰(`callerStaffId`)으로만 안다. 본문·쿼리의 직원 id 를 믿지 않는다. 남의 방문 일정은 없는 것과 같은 응답을 준다.
- 바꾸는 일은 모두 REST 로 한다. WebSocket 은 시세와 "바뀌었다" 알림만 보낸다. 앱 화면은 서버가 아니라 기기 DB 를 본다.
- 화면·유스케이스가 막은 조건(계좌 종류, 가입 적합성·금액·계좌)은 서버도 한 번 더 막는다.
- 계약을 바꿀 때는 `:shared` 를 먼저 고치고 `./gradlew :server:test` 를 돌린다. 서버 테스트는 내장 PostgreSQL 이라 설치할 것이 없다.

## 작업 방식

흔한 코딩 실수를 줄이기 위한 일반 지침이다. **위 규칙과 부딪히면 위 규칙이 우선한다.**
속도보다 신중함 쪽으로 기울어 있으니, 사소한 작업은 판단해서 가볍게 한다.

### 1. 코딩 전에 생각하기

- 가정은 드러내서 말한다. 해석이 여러 개면 조용히 하나를 고르지 말고 제시한다.
- 더 단순한 방법이 있으면 말한다. 요청이 문제 있어 보이면 반대 의견을 낸다.
- **묻는 기준** — 요구사항 해석이 갈리거나, 되돌리기 어려운 결정(사람에게 나가는 동작, 데이터
  삭제, DB 스키마 변경)이면 멈추고 묻는다. 구현 세부는 묻지 않고 규칙 1대로 기존 코드를 따라
  진행한 뒤, 무엇을 가정했는지 알린다.

### 2. 단순하게

- 요청한 것 이상의 기능, 한 번만 쓰는 코드의 추상화, 요청하지 않은 설정·확장성을 넣지 않는다.
- 일어날 수 없는 경우의 예외 처리는 넣지 않는다. 단, 카메라 취소·생체 인증 실패·신분증 진위 확인 실패처럼
  기기와 사람이 만드는 실패는 **일어나는 일**이다.
- 200줄로 쓴 것이 50줄로 될 수 있으면 다시 쓴다. "시니어가 보면 과하다고 할까?" 를 스스로 묻는다.

### 3. 필요한 곳만 고치기

- 요청과 상관없는 옆 코드·주석·포맷을 "개선" 하지 않는다. 고장 나지 않은 것은 리팩토링하지 않는다
  (리팩토링 자체를 요청받은 경우는 제외).
- 관련 없는 죽은 코드를 보면 알리기만 하고 지우지 않는다.
- 내 변경 때문에 쓰이지 않게 된 import·변수·함수는 지운다.
- 기준: 바뀐 줄마다 사용자의 요청으로 거슬러 올라갈 수 있어야 한다.

### 4. 목표를 검증할 수 있게

- 작업을 확인 가능한 목표로 바꾼다.
  - 검증 추가 → 잘못된 입력 테스트를 먼저 쓰고 통과시킨다
  - 버그 수정 → 재현 테스트를 먼저 쓰고 통과시킨다
  - 리팩토링 → 전후로 테스트가 통과하는지 본다
- 여러 단계 작업은 짧은 계획을 먼저 적는다: `1. [단계] → 확인: [무엇으로]`
- 이 프로젝트에서 "끝" 은 규칙 4·7·8 이다 — 양쪽 컴파일, `verifyAll`(단위·ViewModel 시나리오 테스트와
  Android·iOS 실화면 흐름) 통과, 브랜치 커밋·`main` 머지.

잘 되고 있다면 diff 에 불필요한 변경이 줄고, 과하게 짜서 다시 쓰는 일이 줄고, 질문이 실수 뒤가
아니라 구현 전에 나온다.

## 명령

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64   # 양쪽 컴파일
./gradlew :composeApp:testDebugUnitTest      # 단위·ViewModel 시나리오 (JVM, 서버 없이) — 고칠 때마다
./gradlew :server:test                       # 서버 (내장 PostgreSQL)
./gradlew :composeApp:testDebugUnitTest -PfunctionalTest=true --tests "*functest*"   # 실 서버 기능 테스트 (로컬 서버를 띄워 붙는다)
./gradlew :composeApp:verifyAll -PfunctionalTest=true --continue   # 전체: 서버 → JVM → iOS 시뮬레이터 → 실화면 Android → iOS
./gradlew :composeApp:e2eAndroid -PfunctionalTest=true             # 실화면만 (e2eIos 도 있다)
./gradlew :server:runLocal                   # 로컬 서버 http://localhost:8095 (내장 PostgreSQL 54335, 데이터 server/.localdb)
```

- Android Studio 실행 구성: `테스트 (서버 없이)` · `기능테스트 (실 서버)` · `검증 - 빠르게 (JVM)` · `검증 - 전체 (실화면 포함)`
- 서버 모드 앱은 `local.properties` 의 `server.baseUrl`(또는 환경변수 `ODS_SERVER_BASE_URL`)로 빌드한다. 주소는 빌드할 때 박히므로 바꾸면 다시 빌드한다.
  Android 에뮬레이터는 `http://10.0.2.2:8095`, iOS 시뮬레이터는 `http://localhost:8095`. 포트가 겹치면 `-PlocalServerPort=<포트>`
- 검증용 로컬 서버는 빌드가 도는 동안만 뜬다(`:server:startLocalServer`). 이미 떠 있으면 그 서버를 쓴다
- 기기는 `local.properties` 의 `e2e.androidAvd`·`e2e.iosDevice`. 꺼져 있으면 `.maestro/run.sh` 가 켠다
- 결과: `composeApp/build/test-results/`, `build/maestro/<android|ios>/report.xml`

