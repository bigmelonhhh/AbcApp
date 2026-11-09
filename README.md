# AbcApp

咕咕 ABC：给 3 岁宝宝的离线英语启蒙平板 App（Android / Kotlin + Compose）。
android-pad-mobile

## 验证与测试

三层验证职责不同，不能互相替代：

| 层 | 命令 | 覆盖内容 | 需要设备 |
| --- | --- | --- | --- |
| 静态契约走查 | `python tools/verify_project.py` | 68 项：设计规格、资源、V2 词表、交互修复、测试体系自检 | 否 |
| JVM 单元测试 | `./gradlew testDebugUnitTest` | 39 例：`QuizPicker` 干扰项规则、`TimeBudget` 5 分钟时长边界、24 词/8 课与路由契约 | 否 |
| Compose UI 测试 | 见下方 | 27 例：V1 五屏、V2 贴纸册/家长面板/家长门、端到端导航流程 | 是（按需） |
| 1:1 像素验收 | `python tools/verify_screens.py` | V1 五张基准图比对 | 是（荣耀平板 X9） |

测试源码：`app/src/test/`（3 个 JVM 测试类）、`app/src/androidTest/`（3 个 UI 测试类 + `TestHarness`）。
业务规则放在纯 Kotlin 的 `state/QuizPicker.kt` / `state/TimeBudget.kt`，生产状态机
`SessionState.kt` 只做委托，所以单测是真正执行规则，不是字符串检查。

### 中文工程路径的两个坑

1. **单测 classpath 乱码**：Windows 中文路径下 JBR 的 `sun.jnu.encoding=GBK`，Gradle 把
   worker 的 `@argfile` 写成 UTF-8，classpath 里的 `D:\英语学习\...` 被解成乱码，
   报 `ClassNotFoundException`。`build.gradle.kts` 的 `stageUnitTestClasses` 会把
   main + unitTest 的 classes 同步到纯 ASCII 临时目录并追加到 `Test.classpath`，无需手工处理。
2. **AGP 8.9 的 UTP 写设备 profile 失败**：`connectedDebugAndroidTest` 在中文路径下报
   `Invalid file path`（0 tests）；`android.experimental.androidTest.useUnifiedTestPlatform=false`
   在 8.9 已不生效，不要依赖它。要在设备上跑请走下面的手工 instrumentation 通道。

### 在设备上运行 UI 测试（按需）

```bash
cd app
./gradlew assembleDebug assembleDebugAndroidTest
ADB=$ANDROID_HOME/platform-tools/adb
$ADB install -r build/outputs/apk/debug/GuguABC-debug.apk
$ADB install -r build/outputs/apk/androidTest/debug/GuguABC-debug-androidTest.apk
$ADB shell am instrument -w com.guguabc.kids.test/androidx.test.runner.AndroidJUnitRunner
```

`androidx.test` 全家族版本在 `gradle/libs.versions.toml` 成套锁定
（core 1.6.1 / monitor 1.7.1 / runner 1.6.1 / rules 1.6.1 / ext:junit 1.2.1 / espresso 3.6.1）。
monitor 1.7.x 把接口静态方法伴随类改名为 `ActivityInvoker$$CC`，core 若停在 1.5.0 会在运行时
抛 `NoClassDefFoundError: ActivityInvoker$-CC`，因此不要单独升级其中任何一个。

### 发音无声排查

播放链路统一打 `GuguAudio` 日志，`adb logcat -s GuguAudio` 可直接看到
「跳过：音效开关关闭 / 焦点未授予 / 初始化失败 / 播放错误 what+extra / 播放结束」，不用靠猜。
单词卡里「点一下，听发音」整行、上方圆钮、左侧插画底盘都会触发播放。

## 构建

```bash
cd app && ./gradlew assembleDebug assembleRelease
```

release 产物未签名。规格真值源：`design.md`（V1 冻结）、`design_v2.md`（V2）、
`app/tools/word_packs.json`（24 词机器可读）。
