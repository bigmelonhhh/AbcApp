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
}
