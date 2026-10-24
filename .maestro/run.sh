#!/usr/bin/env bash
# 실화면 흐름을 한 플랫폼에 돌린다: 기기를 고르고(꺼져 있으면 켜고) 앱을 설치한 뒤 Maestro 로 .maestro 의 흐름을 돌린다.
#
#   .maestro/run.sh android <apk 경로>
#   .maestro/run.sh ios
#
# 서버 없는 로컬 모드 앱이라 서버를 띄우거나 확인하지 않는다.
# 기기는 환경변수나 local.properties 로 고른다. 없으면 켜져 있는 에뮬레이터·iPhone 시뮬레이터 중 첫 번째를 쓴다.
# 실기기는 고르지 않는다.
#   ODS_ANDROID_AVD / e2e.androidAvd  에뮬레이터 이름. 꺼져 있으면 켠다
#   ODS_IOS_DEVICE  / e2e.iosDevice   시뮬레이터 UDID 또는 이름. 꺼져 있으면 켠다
# 결과는 build/maestro/<플랫폼>/report.xml(JUnit)과 같은 폴더의 스크린샷·로그로 남는다.
set -euo pipefail

IOS_PROJECT=iosApp/iosApp.xcodeproj
IOS_SCHEME=iosApp
IOS_APP=ODS.app
APP_ID=com.eunilsung.ods

PLATFORM=${1:?android 또는 ios}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT=$ROOT/build/maestro/$PLATFORM

fail() { echo "[e2e] $*" >&2; exit 1; }
log() { echo "[e2e] $*" >&2; }

local_property() {
    [ -f "$ROOT/local.properties" ] || return 0
    sed -n "s/^$1=//p" "$ROOT/local.properties" | tail -1 | tr -d '[:space:]'
}

MAESTRO=$(command -v maestro || echo "$HOME/.maestro/bin/maestro")
[ -x "$MAESTRO" ] || fail "Maestro 가 없다. 설치: curl -fsSL https://get.maestro.mobile.dev | bash"

run_flows() {
    rm -rf "$OUT"
    mkdir -p "$OUT"
    log "$PLATFORM($1) 에서 흐름을 돌린다"
    "$MAESTRO" --device "$1" test "$ROOT/.maestro" --format junit --output "$OUT/report.xml" --test-output-dir "$OUT"
}

android_avd_of() { "$ADB" -s "$1" emu avd name 2>/dev/null | head -1 | tr -d '\r'; }

running_emulators() { "$ADB" devices | awk '/^emulator-[0-9]+\tdevice$/ {print $1}'; }

find_android_device() {
    local avd=$1 serial
    if [ -z "$avd" ]; then
        running_emulators | head -1
        return
    fi
    for serial in $(running_emulators); do
        [ "$(android_avd_of "$serial")" = "$avd" ] && { echo "$serial"; return; }
    done
}

boot_android() {
    local avd=$1 serial waited=0
    log "에뮬레이터 $avd 를 켠다"
    nohup "$SDK/emulator/emulator" -avd "$avd" -no-snapshot-save -no-boot-anim -no-audio >/dev/null 2>&1 &
    until serial=$(find_android_device "$avd") && [ -n "$serial" ] &&
        [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
        sleep 3
        waited=$((waited + 3))
        [ $waited -lt 240 ] || fail "에뮬레이터 $avd 가 4분 안에 켜지지 않았다"
    done
    echo "$serial"
}

# 키보드가 한글 자판이면 흐름의 영문 입력이 한글로 바뀐다. 영문(ASCII) 자판이 될 때까지 언어 전환 키를 누른다.
ascii_keyboard() {
    local serial=$1 tries=0 hash
    while [ $tries -lt 6 ]; do
        hash=$("$ADB" -s "$serial" shell settings get secure selected_input_method_subtype | tr -d '\r')
        "$ADB" -s "$serial" shell dumpsys input_method | grep "HashCode=$hash\$" | grep -q "mIsAsciiCapable=true" && return 0
        "$ADB" -s "$serial" shell input keyevent 204
        sleep 1
        tries=$((tries + 1))
    done
    log "영문 자판으로 바꾸지 못했다. 영문 입력 단계가 깨질 수 있다"
}

run_android() {
    local apk=${1:?apk 경로}
    [ -f "$apk" ] || fail "apk 가 없다: $apk"
    SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(local_property sdk.dir)}}
    ADB=$SDK/platform-tools/adb
    [ -x "$ADB" ] || fail "adb 를 찾지 못했다. local.properties 의 sdk.dir 을 확인한다"

    local avd=${ODS_ANDROID_AVD:-$(local_property e2e.androidAvd)}
    local serial
    serial=$(find_android_device "$avd" || true)
    if [ -z "$serial" ]; then
        [ -n "$avd" ] || fail "켜져 있는 에뮬레이터가 없다. e2e.androidAvd 를 주면 켜서 쓴다"
        serial=$(boot_android "$avd")
    fi

    log "$serial 에 설치한다"
    "$ADB" -s "$serial" install -r "$apk" >/dev/null
    ascii_keyboard "$serial"
    run_flows "$serial"
}

find_ios_device() {
    python3 - "$1" <<'PY'
import json, subprocess, sys
want = sys.argv[1]
data = json.loads(subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "-j"]))
devices = [d for runtime, ds in data["devices"].items() if "iOS" in runtime for d in ds]
if want:
    matched = [d for d in devices if want in (d["udid"], d["name"])]
    matched.sort(key=lambda d: d["state"] != "Booted")
else:
    matched = [d for d in devices if d["state"] == "Booted" and d["name"].startswith("iPhone")]
print(matched[0]["udid"] if matched else "")
PY
}

run_ios() {
    local want=${ODS_IOS_DEVICE:-$(local_property e2e.iosDevice)}
    local udid
    udid=$(find_ios_device "$want")
    [ -n "$udid" ] || fail "쓸 시뮬레이터가 없다. e2e.iosDevice 에 UDID 나 이름을 준다"
    xcrun simctl boot "$udid" 2>/dev/null || true
    xcrun simctl bootstatus "$udid" -b >/dev/null

    mkdir -p "$ROOT/build/maestro"
    local build_log=$ROOT/build/maestro/ios-build.log
    log "iOS 앱을 빌드한다 (로그: $build_log)"
    xcodebuild -project "$ROOT/$IOS_PROJECT" -scheme "$IOS_SCHEME" -configuration Debug \
        -sdk iphonesimulator -destination "id=$udid" -derivedDataPath "$ROOT/build/ios-derived" \
        build >"$build_log" 2>&1 ||
        { tail -30 "$build_log" >&2; fail "iOS 빌드가 실패했다"; }

    log "$udid 에 설치한다"
    xcrun simctl terminate "$udid" "$APP_ID" 2>/dev/null || true
    xcrun simctl install "$udid" "$ROOT/build/ios-derived/Build/Products/Debug-iphonesimulator/$IOS_APP"
    run_flows "$udid"
}

case "$PLATFORM" in
    android) run_android "${2:-}" ;;
    ios) run_ios ;;
    *) fail "android 또는 ios 를 준다" ;;
esac
