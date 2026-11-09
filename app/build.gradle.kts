import org.gradle.api.tasks.Sync

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.guguabc.kids"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guguabc.kids"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // design.md §2：V1 全离线，不引任何跨端框架 / 产品风味
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true   // BuildConfig.DEBUG 用于密度日志与调试分支
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            // V1 未接入签名配置，release 产物为 unsigned，供本地验证用
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)   // §7.1 页面切换 200ms 淡入淡出
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // JVM 单元测试：只跑纯逻辑（QuizPicker / TimeBudget / 词表契约），不依赖 Android 运行时
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    // 模拟器 / 真机上的 Compose UI 测试：组件级 + 端到端流程。
    // androidx.test 全家族显式成套声明：Compose ui-test 传递依赖会带来 1.5.x 的 core 与
    // 3.5.x 的 espresso，与 monitor 1.7.x 混用会在运行时找不到 ActivityInvoker$-CC。
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.monitor)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
    // ui-test-manifest 装进主 debug APK，其传递的 core 也必须与测试 APK 同一代。
    debugImplementation(libs.androidx.test.core)
}

// 工程根路径含中文（D:\英语学习）。中文 Windows 上 JBR 的 sun.jnu.encoding=GBK，
// Gradle 把 worker 的 @argfile 写成 UTF-8，JVM 启动时会把 classpath 里所有
// D:\英语学习\... 条目解成乱码 → 单测 ClassNotFoundException（-D 参数无效，
// argfile 在 VM 启动前就已解析完）。根治办法：把本工程 main + unitTest 的
// classes 同步到纯 ASCII 临时目录，并追加到单测 classpath；乱码条目被 JVM
// 静默忽略，不影响任何主构建、打包或真机测试。
val asciiUnitTestClasses: File = File(System.getProperty("java.io.tmpdir"), "gugu-abc-unit-test")

val stageUnitTestClasses by tasks.registering(Sync::class) {
    into(asciiUnitTestClasses)
    dependsOn("compileDebugUnitTestKotlin", "bundleDebugClassesToRuntimeJar")
    val out = layout.buildDirectory.get().asFile
    from(fileTree(File(out, "tmp/kotlin-classes/debugUnitTest")))
    from(zipTree(File(out,
        "intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar")))
}

tasks.withType<Test>().configureEach {
    dependsOn(stageUnitTestClasses)
    doFirst { classpath = files(classpath, asciiUnitTestClasses) }
}
