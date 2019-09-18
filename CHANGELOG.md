### v2.0.5(2019-09-18)
* Replace `variant.getJavaCompiler()` with `variant.getJavaCompileProvider()`

### v2.0.5(2019-02-11)
* Changed `println` usages for a SLF4J logger instance in DEBUG mode

### v2.0.4(2018-10-16)
* [FIX] issue[#120](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/120)
* [FIX] issue[#118](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues/118)

### v2.0.2(2018-08-07)
* [FIX]无法识别kotlin写的Aspect文件

### v2.0.1(2018-05-03)
* 解决dexguard混淆兼容性问题(ClassNotFoundException)

### v2.0.0(2018-04-24)
* 支持Instant Run编译
* 废弃 `includeJarFilter`和`excludeJarFilter`两个配置命令
* 新增 `include`和 `exclude`配置命令，通过包名(package)路径关键字匹配，可过滤class文件和jar文件
* `include`和 `exclude`配置命令支持`*`，`**`通配符
* 修复已知的gradle兼容性Bug
* 提升编译效率

### v1.1.1(2017-12-12)
* fix: no effects while building with java8
* this version disable `includeJarFilter` and `excludeJarFilter`configuration, and will be fixed in the next version.

### v1.1.0(2017-11-02)
* fix: exception occurs on android plugin 3.0.0: "Unexpected scopes found in folder xx, Required: PROJECT, SUB_PROJECTS, EXTERNAL_LIBRARIES. Found: EXTERNAL_LIBRARIES, PROJECT, PROJECT_LOCAL_DEPS, SUB_PROJECTS, SUB_PROJECTS_LOCAL_DEPS

### v1.0.11(2017-03-29)
* fix: mistake about ajcArgs 

### v1.0.10(2017-03-13)
* fix: error on AndroidTest: Getting classpath error: unable to find org.aspectj.lang.JoinPoint when running instrumentation tests #19

### v1.0.9(2016-11-16)
* add configuration aspectjx:ajcArgs
* ignore xlint by default, do not care about error as 'can not determine superclass of missing type...' 
and do not config aspectjx.excludeJarFilter to filter the jar that may cause compile warning and error.

### v1.0.8(2016-10-19)
* fix compatible bug on android plugin 2.2

### v1.0.7(2016-10-18)
* supports android build plugin 2.2

### v1.0.6(2016-08-10)
* fix: java.lang.NoSuchMethodError occurs if app built to Multi flavors and obfuscated with dexguard.

### v1.0.5(2016-07-08)
* fix bug: class lost when build with dexguard

### v1.0.3(2016-06-01)
 *fix duplicate commons-io files error.

### v1.0.3(2016-06-01)
* remove extension aspectjx.jarFilter
* add extension aspectjx.includeJarFilter, aspectjx.excludeJarFilter
* solve slash problem("/", "\\") on windows, unix like system

### v1.0.2(2016-05-04)
* add extensions aspectjx.jarFilter

### v1.0.1(2016-05-04)
* change groupID, artifactID as 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:1.0.1'

### v1.0.0(2016-04-25)

* Initial release.