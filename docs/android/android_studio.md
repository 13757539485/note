### Compose与kotlin版本兼容

https://developer.android.com/jetpack/androidx/releases/compose-kotlin?hl=zh-cn


### <a id="kapt">注解处理器</a>

auto-service: https://github.com/google/auto

添加kapt
```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}
annotationProcessor 'com.google.auto.service:auto-service:1.1.1'
kapt 'com.google.auto.service:auto-service:1.1.1'
```

### gradle常见问题

#### 老版本升级gradle8.0

1.先修改gradle/gradle-wrapper.properties成8.0
```gradle
distributionBase=GRADLE_USER_HOME
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
distributionPath=wrapper/dists
zipStorePath=wrapper/dists
zipStoreBase=GRADLE_USER_HOME
```

2.修改settings.gradle和根build.gradle文件

build.gradle参考模板
```gradle
buildscript {
    ext {
        compose_version = '1.4.2'
    }
}
plugins {
    id 'com.android.application' version '8.0.0' apply false
    id 'com.android.library' version '8.0.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.8.10' apply false
    id 'org.jetbrains.kotlin.jvm' version '1.8.10' apply false
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```
settings.gradle参考模板
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }//三方库常用
    }
}
rootProject.name = "KotlinApp"//项目文件夹名字
include ':app'
include ':other'//其他module
```
其他报错修改

1.Build was configured to prefer settings repositories over project repositories but repository 'flatDir' was added by build file 'xxx\build.gradle'
```gradle
repositories {
    flatDir {
        dirs 'libs', 'providedLibs'
    }
}
```
解决方式

将flatDir部分代码移动到settings.gradle

2.Namespace not specified. Please specify a namespace in the module's build.gradle file like so

将每个build.gradle中添加包名，可从AndroidManifest中查看
```gradle
android {
    namespace 'com.xxx.xxx'
}
```

3.Build Type 'debug' contains custom BuildConfig fields, but the feature is disabled.

build.gradle中使用了自定义配置(BuildConfig)属性buildConfigField，8.0默认关闭需要手动开启
```gradle
android {
    buildFeatures {
        buildConfig = true
    }
}
```
4.<a id="aidl-create">无法新建AIDL文件</a>
Requires setting the buildFeatures.aidl to true in the build file
```gradle
buildFeatures {
    aidl = true
}
```

### <a id="ndk_config">ndk配置</a>
```gradle
android {
    //...
    defaultConfig {
        //...
        //配置自己c++代码编译的CPU架构
        externalNativeBuild {
            cmake {
                cppFlags ''
                abiFilters 'armeabi-v7a'
            }
        }
        //配置打包哪些CPU架构
        ndk {
            abiFilters 'armeabi-v7a'
        }
    }
    //...
    //配置cpp代码路径
    externalNativeBuild {
        cmake {
            path file('src/main/cpp/CMakeLists.txt')
            version '3.22.1'
        }
    }
}
```
as插件：CMake simple highlighter

### gradle下载源

https://mirrors.huaweicloud.com/repository/maven/

http://mirrors.cloud.tencent.com/gradle/

替换：
.gradle/wrapper/dists/

### 常见异常

1. 提示此应用专为旧版android打造...，底部显示全屏

    将minSdkVersion修改成24或以上

2. java转kt后启动Activity失败

    app项目的gradle中加kotlin插件
```gradle
plugins {
    //...
    id 'org.jetbrains.kotlin.android'
}
```
根目录的gradle对应
```gradle
plugins {
    //...
    id 'org.jetbrains.kotlin.android' version '1.8.10' apply false
}
```