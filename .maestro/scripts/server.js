/**
 * 화면 밖에서 서버를 준비한다. 앱에는 test1 로 로그인하고, 여기서는 같은 직원으로 REST 만 부른다.
 *
 * ACTION 으로 할 일을 고른다.
 * - ensureTodayVisit: test1 에게 오늘(한국 시간) 방문 일정이 하나도 없으면 하나 잡는다.
 *   서버의 시연용 일정은 서버 DB 를 처음 채운 날을 기준으로 놓여, 며칠 지나면 오늘 일정이 비어 있을 수 있다.
 *   이미 있으면 아무것도 하지 않는다 — 실행마다 일정이 쌓이지 않는다.
 * - newCustomer: 계좌가 없는 새 고객을 등록하고 output.name·output.search(전화번호 숫자 8자리)·output.rrnFront·output.rrnBack 을 준다.
 *   서버 DB 는 실행이 끝나도 남고 지우는 길이 없어, 시연용 고객은 지난 실행이 연 계좌를 갖고 있다. 실행마다 새 고객을 쓴다.
 *   이름은 영문이다 — Android 의 Maestro 는 한글을 입력하지 못하는데, 신분증 이름 칸에 고객 이름을 그대로 넣어야 한다.
 *
 * 서버 주소는 run.sh 가 `-e ODS_SERVER=` 로 넘긴다. 없으면 로컬 서버다.
 */
const BASE = typeof ODS_SERVER === 'undefined' ? 'http://localhost:8095' : ODS_SERVER
const ME = 'test1'
const PASSWORD = '1234'
const VISIT_CUSTOMER = 'C003'
const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR
const SEOUL_OFFSET = 9 * HOUR

function call(method, path, token, body) {
    const options = { headers: { 'Content-Type': 'application/json' } }
    if (token) options.headers.Authorization = 'Bearer ' + token
    if (method !== 'GET') options.body = body === undefined ? '' : JSON.stringify(body)
    const response = method === 'GET' ? http.get(BASE + path, options) : http.post(BASE + path, options)
    if (response.status >= 300) throw new Error(method + ' ' + path + ' → ' + response.status + ' ' + response.body)
    return response.body ? json(response.body) : null
}

function login() {
    return call('POST', '/auth/login', null, { id: ME, password: PASSWORD }).token
}

/** 실화면 흐름이 끝 네 자리로 찾는 시연용 고객과 섞이지 않는 전화번호. */
function uniquePhone() {
    for (let seed = Date.now(); ; seed++) {
        const middle = '6' + String(Math.floor(seed / 10000) % 1000).padStart(3, '0')
        const last = String(seed % 10000).padStart(4, '0')
        const digits = '010' + middle + last
        if (!['4587', '3302', '7731'].some(d => digits.indexOf(d) >= 0)) return { middle: middle, last: last }
    }
}

function newCustomer() {
    const token = login()
    try {
        const phone = uniquePhone()
        const customer = call('POST', '/customers', token, {
            name: 'E2E Open ' + phone.last,
            birthDate: '1990-05-09',
            gender: 'MALE',
            phone: '010-' + phone.middle + '-' + phone.last,
            address: 'E2E',
        })
        output.name = customer.name
        output.search = phone.middle + phone.last
        output.rrnFront = '900509'
        output.rrnBack = '1'
    } finally {
        call('POST', '/auth/logout', token)
    }
}

function ensureTodayVisit() {
    const token = login()
    try {
        const start = Math.floor((Date.now() + SEOUL_OFFSET) / DAY) * DAY - SEOUL_OFFSET
        const visits = call('GET', '/visits', token)
        if (!visits.some(v => v.scheduledAtMillis >= start && v.scheduledAtMillis < start + DAY)) {
            call('POST', '/visits', token, {
                customerId: VISIT_CUSTOMER,
                scheduledAtMillis: start + 20 * HOUR,
                place: 'E2E',
                purpose: 'CONSULT',
                memo: '',
            })
        }
    } finally {
        call('POST', '/auth/logout', token)
    }
}

const actions = { ensureTodayVisit, newCustomer }
if (!actions[ACTION]) throw new Error('모르는 ACTION: ' + ACTION)
actions[ACTION]()
