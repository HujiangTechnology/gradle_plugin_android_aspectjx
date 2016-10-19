[xposed]:https://github.com/rovo89/Xposed
[dexposed]:https://github.com/alibaba/dexposed
[Hugo]:https://github.com/JakeWharton/hugo
[gradle-android-aspectj-plugin]:https://github.com/uPhyca/gradle-android-aspectj-plugin
[question issue]:https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues

aspectjx
==================================

##跟原版的区别：（pr 已提）
在原有的基础上兼容了 com.android.tools.build:gradle:2.2.0  
问题类如下：  
com.android.utils.FileUtils  
com.android.build.gradle.internal.transforms.JarMerger  
com.android.builder.signing.SignedJarBuilder  
代码直接从原来的类中抽取出来。  
由于pr 还没被合进去。如果你是使用的是2.2.0 的版本，并且你想马上使用。可以使用我提供的插件。  
期待pr能被合进去。。。  
build.gradle  
~~~
buildscript {
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }

    dependencies {
        classpath 'com.android.tools.build:gradle:2.2.0'
        classpath 'com.github.zzz40500:gradle_plugin_android_aspectjx:v1.0.7'
        classpath 'org.aspectj:aspectjtools:1.8.+'
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
~~~
app/build.gradle  
~~~
apply plugin: 'android-aspectjx'
~~~




A gradle plugin that supports using AspectJ in android project. can weave third party libs and [kotlin code](https://kotlinlang.org/)

## [中文版本](README-zh.md)

## why born

There is no perfect AOP tools or framework on android, even though [xposed] and [dexposed] are fantastic libs, but not compatible for all Android system versions.And some of the AspectJ plugins can not work well on libs(aar, jar) and kotlin code.

**aspectjx** takes some great ideas from **JakeWharton**'s project [Hugo] and **uPhyca**'s [gradle-android-aspectj-plugin], and extends the ability to support aar, jar, kotlin. Thanks to **JakeWharton** and **uPhyca**.

## How to use

> Before using **aspectjx**, Android gradle plugin version must be greater than or equal `1.5.0`, and **aspectjx** must be used in `application module`, not `library module`.

#### i. dependency

```
dependencies {
        classpath 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:1.0.7'
        }
```

OR

using **JAR** from [product](product/). Make new directory named as **plugins**, and put **product/gradle-android-plugin-aspectjx-1.0.7.jar** in **plugins**,
 then do as below:

```
dependencies {
        classpath fileTree(dir:'plugins', include:['*.jar'])
        //don't lost dependency
        classpath 'org.aspectj:aspectjtools:1.8.+'
        }
```

`ATTENTION`: `compile 'org.aspectj:aspectjrt:1.8.+'` must be added to the modules that contain the AspectJ code, [read the Demo](https://github.com/HujiangTechnology/AspectJX-Demo/blob/master/library/build.gradle)


#### ii. using in application module

```
apply plugin: 'android-aspectjx'

```
#### iii. aspectjx extension configuration

**aspectjx** will scan and weave all **.class** file and **jar**, **aar** by the default，except for add some filter config **includeJarFilter**, **excludeJarFilter**. **includeJarFilter**, **excludeJarFilter** can support **groupId** filter, **artifactId** filter and **dependency path** matching.

```
aspectjx {
	//includes the libs that you want to weave
	includeJarFilter 'universal-image-loader', 'AspectJX-Demo/library'
	
	//excludes the libs that you don't want to weave
	excludeJarFilter 'universal-image-loader'
}
```

* excludes the lib whose groupId is `org.apache.httpcomponents`

```
aspectjx {
	excludeJarFilter 'org.apache.httpcomponents'
}
```
* exlucdes the lib whose artifactId is `gson`

```
	aspectjx {
		excludeJarFilter 'gson'
	}
```

* excludes the jar `alisdk-tlog-1.jar`

```
	aspectjx {
		excludeJarFilter 'alisdk-tlog-1'
	}
```

* excludes all dependency libs

```
aspectjx {
	excludeJarFilter '.jar'
}
```

## Attention
* IntelliJ now has no tools for AspectJ, Just Annotation Style AspectJ can work on Android studio.  *.aj file can not be compiled . [How to use Annotation Style AspectJ](https://github.com/HujiangTechnology/AspectJ-Demo)
* AspectJ may not work well on Android studio with Instant Run feature, if so, close the Instant Run feature.
* **AspectJ** may compile error as below, just exlcudes the associated lib to resolve it.

![](docs/aspectj_err_0.png)

* The Classes and methods that you want to hooked in AOP should not be obfuscated:

```
package com.hujiang.test;

public class A {
    public boolean funcA(String args) {
        ....
    }
}

//if you want to hook A#funcA(String) in AOP code, keep this class and method in proguard config file

-keep class com.hujiang.test.A {*;}

```




## [Feedback](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues)


## [CHANGELOG](CHANGELOG.md)


## Reference


* [How to use Annotation Style AspectJ Demo](https://github.com/HujiangTechnology/AspectJ-Demo)
* [Android M permission lib with aspectjx](https://github.com/firefly1126/android_permission_aspectjx)


* [AspectJ](https://eclipse.org/aspectj/)

* [AspectJ Programming Guide](https://eclipse.org/aspectj/doc/released/progguide/index.html)

* [AspectJ Development Environment Guide](https://eclipse.org/aspectj/doc/released/devguide/index.html)

* [AspectJ NoteBook](https://eclipse.org/aspectj/doc/released/adk15notebook/index.html)

## Contact


email:xiaoming1109@gmail.com

QQ:541136835

wechat:13386016339


## License


    Copyright 2016 firefly1126, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.gradle_plugin_android_aspectjx




