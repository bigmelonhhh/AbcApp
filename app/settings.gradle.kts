// 国内镜像优先（2026-11-07 实测各仓库关键 artifact 均返回 200）。
// 官方源保留在末尾作兜底：Gradle 按声明顺序查找，镜像缺包时会自动回退，不会直接构建失败。
// Gradle 发行包镜像见 gradle/wrapper/gradle-wrapper.properties（腾讯云 / 华为云）。
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "GuguABC"
