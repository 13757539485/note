### 创建项目
Android Studio创建普通应用项目如SystemUIPlugin
### 添加到系统目录
[内置系统应用](environment.md#vendor_app)，编写Android.bp(mk可参考[mk转bp](environment.md#mk_bp)并添加到编译模块中，在[vendor定制](environment.md#vendor)中添加SystemUIPlugin模块
```mk
android_app_import {
    name: "SystemUIPlugin",
    certificate: "platform",
    dex_preopt: {
        enabled: true,
    },
    system_ext_specific: true,
	privileged: true,
    enforce_uses_libs: false,
    apk: "app/build/outputs/apk/release/app-release.apk",
}
```
- 注：此处必须使用gradle先编译出apk才能使用m SystemUIPlugin或者整体编译
### gradle添加系统签名
[获取系统签名](environment.md#system_sign)platform.jks，[gradle引用签名](../../android_studio.md#gradle_8.9)

### 引用fw接口
out/target/common/obj/JAVA_LIBRARIES/framework-minus-apex_intermediates/classes.jar

out/soong/.intermediates/frameworks/base/framework-minus-apex/android_common/7bd916565615329d50364be05485f3c9/combined/framework.jar

编译出framework.jar文件，[编译命令](../fws_aosp_make.md#name_mod)，放到项目libs中，gradle配置
```kts
gradle.projectsEvaluated {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-Xbootclasspath/p:${rootProject.projectDir}/${project.name}/libs/framework.jar")
    }
}

dependencies {
    compileOnly(files("libs/framework.jar"))
}

tasks.register("fwBuild") {
    val imlFile =
        file("../.idea/modules/${project.name}/${rootProject.name}.${project.name}.main.iml")
    if (!imlFile.exists()) {
        println("IML file not found: ${imlFile.absolutePath}")
        return@register
    }

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(imlFile)
    val parsedXml = document.documentElement

    val componentNodes = parsedXml.getElementsByTagName("component")
    val newModuleRootManager = (0 until componentNodes.length)
        .asSequence()
        .map { componentNodes.item(it) as Element }
        .firstOrNull { it.getAttribute("name") == "NewModuleRootManager" }

    if (newModuleRootManager == null) {
        println("NewModuleRootManager component not found")
        return@register
    }

    val orderEntries = newModuleRootManager.getElementsByTagName("orderEntry")
    val sdkEntry = (0 until orderEntries.length)
        .asSequence()
        .map { orderEntries.item(it) as Element }
        .firstOrNull {
            it.getAttribute("type") == "jdk" && it.getAttribute("jdkName").startsWith("Android API")
        }

    val frameworkEntry = (0 until orderEntries.length)
        .asSequence()
        .map { orderEntries.item(it) as Element }
        .firstOrNull {
            it.getAttribute("type") == "library" && it.getAttribute("name").contains("framework")
        }

    if (sdkEntry != null && frameworkEntry != null) {
        newModuleRootManager.removeChild(frameworkEntry)
        newModuleRootManager.insertBefore(frameworkEntry, sdkEntry)

        val transformer = TransformerFactory.newInstance().newTransformer()
//        transformer.setOutputProperty(OutputKeys.INDENT, "yes") // 启用缩进
//        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4") // 设置缩进为 4 个空格
        // 将修改后的 DOM 写回 .iml 文件
        transformer.transform(DOMSource(document), StreamResult(FileWriter(imlFile)))
        println("IML file updated successfully")
    } else {
        println("SDK entry or framework entry not found")
    }
}

tasks.named("preBuild") {
    dependsOn("fwBuild")
}
```
<font color="#dd0000">注：</font>
- fwBuild这个task执行中需要检查和修改.idea的路径是否正确，idea没有生成module相关[开启idea配置](../../android_studio.md#enable_idea)
- gradle中的java版本需要使用1.8

```kts
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
```
### 集成到fw
[生成jar](../../android_studio.md#gradle_jar)

[framework引用jar](../fws_mk_bp.md#import_fws)

framework：只能引用jar，无法引用aar

