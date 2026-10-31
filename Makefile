# Slate 本地开发快捷命令。运行 `make` 或 `make help` 查看全部。

ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB          := $(ANDROID_HOME)/platform-tools/adb
EMULATOR     := $(ANDROID_HOME)/emulator/emulator
AVD          ?= slate_tv
SERIAL       ?= emulator-5554
APP_ID       := dev.minorcell.slate
ACTIVITY     := $(APP_ID)/.ui.HomeActivity
SHOT_DIR     := build/screenshots
SHOT         := $(SHOT_DIR)/home-$(shell date +%H%M%S).png

.PHONY: help emu emu-stop install start run shot logs test check clean

help: ## 显示可用命令
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-10s %s\n", $$1, $$2}'

emu: ## 启动模拟器并等待开机完成
	@if $(ADB) -s $(SERIAL) get-state >/dev/null 2>&1; then \
		echo "$(SERIAL) 已在运行"; \
	else \
		nohup $(EMULATOR) -avd $(AVD) -gpu auto -no-boot-anim >/dev/null 2>&1 & \
		echo "启动 $(AVD)，等待开机…"; \
		$(ADB) -s $(SERIAL) wait-for-device; \
		until [ "$$($(ADB) -s $(SERIAL) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 1; done; \
		echo "开机完成"; \
	fi

emu-stop: ## 关闭模拟器
	-$(ADB) -s $(SERIAL) emu kill

install: ## 构建并安装到模拟器（不会装到电视）
	ANDROID_SERIAL=$(SERIAL) ./gradlew -q :app:installDebug
	@echo "已安装到 $(SERIAL)"

start: ## 在模拟器上启动 Slate
	$(ADB) -s $(SERIAL) shell am start -n $(ACTIVITY)

run: install start ## 安装、启动并截图，一次看到改动
	@sleep 2
	@mkdir -p $(SHOT_DIR)
	$(ADB) -s $(SERIAL) exec-out screencap -p > $(SHOT)
	@echo "截图：$(SHOT)"

shot: ## 截图到 build/screenshots/
	@mkdir -p $(SHOT_DIR)
	$(ADB) -s $(SERIAL) exec-out screencap -p > $(SHOT)
	@echo "截图：$(SHOT)"

logs: ## 查看应用日志
	$(ADB) -s $(SERIAL) logcat -v time | grep -E "minorcell|AndroidRuntime"

test: ## 运行单元测试
	./gradlew test

check: ## 运行 ktlint、detekt 与单元测试
	./gradlew ktlintCheck detekt test

clean: ## 清理构建产物与截图
	./gradlew clean
	rm -rf $(SHOT_DIR)
