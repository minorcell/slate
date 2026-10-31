#!/usr/bin/env bash
#
# 构建并通过 adb 替换电视上的桌面程序。
#
# 顺序保证安全：
#   1. 构建 debug APK
#   2. 安装到电视
#   3. 设为默认 HOME 并启动
#   4. 确认新桌面确实在前台
#   5. 只有第 4 步成功，才删除旧桌面
#
# 用法:
#   ./script/replace.sh <电视IP[:端口]>
#   OLD_PACKAGE=dev.lite.launcher ./script/replace.sh 192.168.1.71:5555
#   OLD_PACKAGE= ./script/replace.sh 192.168.1.71:5555   # 不删除任何旧程序
#
# 环境变量:
#   OLD_PACKAGE  要删除的旧桌面包名，默认为空（不删除）

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NEW_PACKAGE="dev.minorcell.slate"
NEW_ACTIVITY="$NEW_PACKAGE/$NEW_PACKAGE.ui.HomeActivity"
OLD_PACKAGE="${OLD_PACKAGE:-}"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

log() { printf '\033[36m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[33mwarn:\033[0m %s\n' "$*"; }
die() {
  printf '\033[31merror:\033[0m %s\n' "$*" >&2
  exit 1
}

SERIAL="${1:-}"
if [ -z "$SERIAL" ]; then
  printf '用法: %s <电视IP[:端口]>\n  例如: %s 192.168.1.71:5555\n' "$0" "$0" >&2
  exit 1
fi
case "$SERIAL" in
  *:*) ;;
  *) SERIAL="$SERIAL:5555" ;;
esac

command -v adb >/dev/null 2>&1 || die "找不到 adb，请先安装 Android platform-tools"
[ -x "$ROOT/gradlew" ] || die "找不到可执行的 gradlew"

log "连接电视 $SERIAL"
adb connect "$SERIAL" >/dev/null
adb -s "$SERIAL" wait-for-device

log "构建 debug APK"
(cd "$ROOT" && ./gradlew assembleDebug)
[ -f "$APK" ] || die "构建产物不存在：$APK"

log "安装到电视"
adb -s "$SERIAL" install -r "$APK"

log "设为默认桌面"
adb -s "$SERIAL" shell cmd package set-home-activity "$NEW_ACTIVITY" >/dev/null

log "启动新桌面"
adb -s "$SERIAL" shell am force-stop "$NEW_PACKAGE"
adb -s "$SERIAL" shell am start -n "$NEW_ACTIVITY" >/dev/null
sleep 3

RESUMED="$(adb -s "$SERIAL" shell dumpsys activity activities | grep -m1 mResumedActivity || true)"
if ! printf '%s' "$RESUMED" | grep -q "$NEW_PACKAGE"; then
  die "新桌面没有在前台，已保留旧程序。当前前台：$RESUMED"
fi
log "新桌面已在前台"

if [ -z "$OLD_PACKAGE" ]; then
  log "未指定旧程序，跳过删除"
  exit 0
fi

if adb -s "$SERIAL" shell pm path "$OLD_PACKAGE" >/dev/null 2>&1; then
  log "删除旧桌面 $OLD_PACKAGE"
  adb -s "$SERIAL" uninstall "$OLD_PACKAGE"
else
  warn "旧桌面 $OLD_PACKAGE 不存在，无需删除"
fi

log "完成"
