[xposed]:https://github.com/rovo89/Xposed
[dexposed]:https://github.com/alibaba/dexposed
[Hugo]:https://github.com/JakeWharton/hugo
[gradle-android-aspectj-plugin]:https://github.com/uPhyca/gradle-android-aspectj-plugin

gradle_plugin_android_aspectjx
==================================

 一个在Android中应用Aspectj的Gradle插件。支持切AAR, JAR， 支持现在Android上最火的Kotlin。 
 
 
 开发该项目的原因是基于还没有发现目前的开源库中比较好的AOP框架或者工具，虽然[xposed]，[dexposed]非常强大，但由于Android的碎片化比较严重，兼容问题永远是一座无法逾越的大山。而且发现的AspectJ相关插件都不支持AAR或者JAR切入的，对于目前在Android圈很火爆的Kotlin更加无能为力。
 
 该项目的设计参考了大神**JakeWharton**的[Hugo]项目及**uPhyca**的[gradle-android-aspectj-plugin]项目的设计思想，并在它们的基础上扩展支持AAR, JAR及Kotlin的应用。在此感谢JakeWharton和uPhyca.[跪拜]


使用
-----

> **gradle_plugin_android_aspectjx**是基于 gradle android插件1.5及以上版本设计的，如果你还在用1.3或者更低版本，请把版本升上去。

> gradle_plugin_android_aspectjx是使用在application module的插件, 虽然用在library module上也不会出错,但是不生效。

* 路径依赖

```
 dependencies {
        classpath 'com.hujiang.gradle.plugin.android:aspectjx:1.0.1'
        }
```
* 或者使用product目录下的jar包，在你的项目根目录下新建目录plugins，把product/aspectx-1.0.1.jar拷贝到plugins，依赖jar包

```
dependencies {
        classpath fileTree(dir:'plugins', include:['*.jar'])
        }
```

* 在app项目的build.gradle里应用插件

```
apply plugin: 'android-aspectjx'
//或者这样也可以
apply plugin: 'com.hujiang.android-aspectjx'
```

**到此为止，gradle_plugin_android_aspectjx的接入就完成了，但是要AspectJ发挥作用还需要你自己写切片代码，可以参考[AspectJ Demo](https://github.com/HujiangTechnology/AspectJ-Demo)**

**不了解AspectJ的请自行了解，参考：**

[AspectJ官网](https://eclipse.org/aspectj/)

[AspectJ Programming Guide](https://eclipse.org/aspectj/doc/released/progguide/index.html)

[AspectJ Development Environment Guide](https://eclipse.org/aspectj/doc/released/devguide/index.html)

[AspectJ NoteBook](https://eclipse.org/aspectj/doc/released/adk15notebook/index.html)


Contact
----------

email:xiaoming1109@gmail.com

QQ:541136835

微信:13386016339


License
-------

    Copyright 2016 hujiang, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.gradle_plugin_android_aspectjx
