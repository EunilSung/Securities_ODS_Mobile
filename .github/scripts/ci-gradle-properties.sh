#!/usr/bin/env bash
# 저장소의 gradle.properties 는 개발 PC 기준(Gradle 힙 8GB · Kotlin 데몬 6GB)이다. GitHub 러너는 메모리가 작아(macOS 7GB)
# 사용자 수준 gradle.properties 로 낮춘다. 이 파일이 저장소 것보다 먼저라 Xcode 빌드 단계가 부르는 Gradle 에도 적용된다.
set -euo pipefail
mkdir -p "$HOME/.gradle"
cat >> "$HOME/.gradle/gradle.properties" <<'PROPS'
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx2g
kotlin.compiler.execution.strategy=in-process
PROPS
