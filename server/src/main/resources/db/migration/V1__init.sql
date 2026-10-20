-- 직원·토큰과 지점 고객 장부, 상품·방문 일정·관심종목.
--
-- 열거형 값은 앱 도메인 이름을 글자로 둔다. 허용하는 값을 CHECK 로 묶어 엉뚱한 값이 들어오지 않게 한다.

-- 영업 직원. id 가 로그인 아이디(소문자)다. 비밀번호는 해시로만 둔다.
CREATE TABLE staff (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    branch        TEXT NOT NULL,
    position      TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 인증 토큰. 토큰 자체가 아니라 SHA-256 해시를 저장한다.
CREATE TABLE auth_token (
    token_hash   TEXT PRIMARY KEY,
    staff_id     TEXT NOT NULL REFERENCES staff (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ
);

-- 고객. 지점 장부라 모든 직원이 함께 본다. 투자성향과 진단 시각은 함께 있거나 함께 없다.
CREATE TABLE customer (
    id               TEXT PRIMARY KEY,
    name             TEXT NOT NULL CHECK (name <> ''),
    birth_date       DATE NOT NULL,
    gender           TEXT NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
    phone            TEXT NOT NULL,
    address          TEXT NOT NULL,
    grade            TEXT NOT NULL CHECK (grade IN ('VIP', 'GOLD', 'GENERAL')),
    risk_profile     TEXT CHECK (risk_profile IN ('STABLE', 'STABLE_SEEKING', 'NEUTRAL', 'ACTIVE', 'AGGRESSIVE')),
    risk_assessed_at TIMESTAMPTZ,
    registered_at    TIMESTAMPTZ NOT NULL,
    CHECK ((risk_profile IS NULL) = (risk_assessed_at IS NULL))
);

-- 계좌. 계좌번호는 서버가 `7001-<휴대폰 가운데 4자리>-<순번>` 으로 발급한다.
CREATE TABLE account (
    account_no  TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    type        TEXT NOT NULL CHECK (type IN ('BROKERAGE', 'CMA', 'ISA', 'PENSION')),
    opened_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX account_customer_idx ON account (customer_id);

-- 상담 이력. 직원 이름은 남길 때의 이름을 그대로 둔다.
CREATE TABLE consultation (
    id           TEXT PRIMARY KEY,
    customer_id  TEXT NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    consulted_at TIMESTAMPTZ NOT NULL,
    channel      TEXT NOT NULL CHECK (channel IN ('VISIT', 'PHONE', 'BRANCH')),
    topic        TEXT NOT NULL CHECK (topic IN ('GENERAL', 'MARKET', 'ACCOUNT_OPENING', 'RISK_PROFILE', 'PRODUCT')),
    content      TEXT NOT NULL,
    staff_name   TEXT NOT NULL
);
CREATE INDEX consultation_customer_idx ON consultation (customer_id, consulted_at DESC);

-- 판매 상품. 핵심설명서 항목은 순서가 있는 글자 목록(JSON 배열)이다.
CREATE TABLE product (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    type            TEXT NOT NULL CHECK (type IN ('FUND', 'ELS', 'BOND', 'RP', 'ISA')),
    risk_grade      TEXT NOT NULL CHECK (risk_grade IN ('VERY_HIGH', 'HIGH', 'MODERATE', 'LOW', 'VERY_LOW', 'MINIMAL')),
    summary         TEXT NOT NULL,
    expected_return TEXT NOT NULL,
    fee_info        TEXT NOT NULL,
    min_amount      BIGINT NOT NULL CHECK (min_amount > 0),
    key_points      JSONB NOT NULL,
    is_complex      BOOLEAN NOT NULL,
    sort_order      INT NOT NULL
);

-- 상품 가입. 계좌는 그 고객의 것이어야 한다(서버가 확인한다).
CREATE TABLE subscription (
    id                   TEXT PRIMARY KEY,
    customer_id          TEXT NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    product_id           TEXT NOT NULL REFERENCES product (id),
    account_no           TEXT NOT NULL REFERENCES account (account_no),
    amount               BIGINT NOT NULL CHECK (amount > 0),
    subscribed_at        TIMESTAMPTZ NOT NULL,
    unsuitable_confirmed BOOLEAN NOT NULL,
    cooling_off_until    TIMESTAMPTZ
);
CREATE INDEX subscription_customer_idx ON subscription (customer_id);

-- 서명. 서명 그림(획 목록)은 서버에만 남고, 앱은 지문(digest)만 둔다.
CREATE TABLE signature (
    id          TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    purpose     TEXT NOT NULL CHECK (purpose IN ('RISK_PROFILE', 'ACCOUNT_OPENING', 'PRODUCT_SUBSCRIPTION')),
    signed_at   TIMESTAMPTZ NOT NULL,
    strokes     JSONB NOT NULL,
    digest      TEXT NOT NULL
);
CREATE INDEX signature_customer_idx ON signature (customer_id);

-- 방문 일정. 직원마다 자기 일정만 본다.
CREATE TABLE visit (
    id           TEXT PRIMARY KEY,
    staff_id     TEXT NOT NULL REFERENCES staff (id) ON DELETE CASCADE,
    customer_id  TEXT NOT NULL REFERENCES customer (id) ON DELETE CASCADE,
    scheduled_at TIMESTAMPTZ NOT NULL,
    place        TEXT NOT NULL,
    purpose      TEXT NOT NULL CHECK (purpose IN ('CONSULT', 'ACCOUNT_OPENING', 'PRODUCT', 'RISK_PROFILE')),
    memo         TEXT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELED'))
);
CREATE INDEX visit_staff_idx ON visit (staff_id, scheduled_at);

-- 관심종목 시세. 시세 흉내가 현재가·거래량을 고쳐 쓴다.
CREATE TABLE stock (
    code           TEXT PRIMARY KEY,
    name           TEXT NOT NULL,
    market         TEXT NOT NULL CHECK (market IN ('KOSPI', 'KOSDAQ')),
    price          BIGINT NOT NULL CHECK (price > 0),
    previous_close BIGINT NOT NULL CHECK (previous_close > 0),
    volume         BIGINT NOT NULL CHECK (volume >= 0),
    sort_order     INT NOT NULL
);
