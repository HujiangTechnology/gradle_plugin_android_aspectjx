### v1.1.0(2017-10-24)
* fix: compat gradle plugin 3.0.0

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