[xposed]:https://github.com/rovo89/Xposed
[dexposed]:https://github.com/alibaba/dexposed
[Hugo]:https://github.com/JakeWharton/hugo
[gradle-android-aspectj-plugin]:https://github.com/uPhyca/gradle-android-aspectj-plugin
[问题反馈]:https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues

AspectJX
==================================

 一个基于AspectJ并在此基础上扩展出来可应用于Android开发平台的AOP框架，可作用于java源码，class文件及jar包，同时支持kotlin的应用。 
 
## 最近更新

#### v2.0.10 (2020-03-31)
* Supports android gradle plugin 3.6.1
* Upgrade inner aspectjrt version to 1.9.5


[查看更多版本信息](CHANGELOG.md)

**AspectJX** 2.0.0版本与旧版本之间编译性能对比数据
> 下面的数据来自于同一个项目不同环境下的编译情况
> 由于旧版本不支持Instant Run增量编译，故没有这块数据

|gradle version|android plugin version|full build(2.0.0/1.1.1 ms)|instant run(2.0.0/1.1.1 ms)|性能提升|
|---|---|---|---|---|
|2.14.1|2.2.0|9761/13213|2596/-|+35%|
|3.3|2.3.0|8133/15306|890/-|+88%|
|4.1|3.0.1|6681/15306|713/-|129%|
|4.4|3.1.4||||

## 如何使用

> **AspectJX**是基于 gradle android插件1.5及以上版本设计使用的，如果你还在用1.3或者更低版本，请把版本升上去。

> 本使用说明是基于重构后的2.0.0版本编写的，如需要查阅旧版本的README，请切换到对应的Tag。
> 

* **插件引用**

在项目根目录的build.gradle里依赖**AspectJX**

```
 dependencies {
        classpath 'com.hujiang.aspectjx:gradle-android-plugin-aspectjx:2.0.8'
        }
```

或者使用product目录下的jar包，在你的项目根目录下新建目录plugins，把product/gradle-android-plugin-aspectjx-2.0.0.jar拷贝到plugins，依赖jar包

```
dependencies {
        classpath fileTree(dir:'plugins', include:['*.jar'])
        }
```

`注意`: 


1. 区别于旧版本，离线新版本不再需要依赖`org.aspectj:aspectjtools:1.8.+`
2. `compile 'org.aspectj:aspectjrt:1.8.+'` 必须添加到包含有AspectJ代码的module. [可以参考Demo](https://github.com/HujiangTechnology/AspectJX-Demo/blob/master/library/build.gradle)


* **在app项目的build.gradle里应用插件**

```
apply plugin: 'android-aspectjx'
//或者这样也可以
apply plugin: 'com.hujiang.android-aspectjx'
```

* **AspectJX配置**

**AspectJX**默认会处理所有的二进制代码文件和库，为了提升编译效率及规避部分第三方库出现的编译兼容性问题，**AspectJX**提供`include`,`exclude`命令来过滤需要处理的文件及排除某些文件(包括class文件及jar文件)。
> 注意：2.0.0版本之后旧版本的`includeJarFilter`和`excludeJarFilter`命令废弃，不再支持使用

> 2.0.0版本的 `include`,`exclude`通过package路径匹配class文件及jar文件，不再支持通过jar物理文件路径匹配的方式，比如：

**支持**

```
aspectjx {
//排除所有package路径中包含`android.support`的class文件及库（jar文件）
	exclude 'android.support'
}
```
**不支持**

```
aspectjx {
	excludeJarFilter 'universal-image-loader'
}

//或者
aspectjx {
	exclude 'universal-image-loader'
}
```


**支持`*`和`**`匹配**

```
aspectjx {
//忽略所有的class文件及jar文件，相当于AspectJX不生效
	exclude '*'
}
```

**提供enabled 开关**

`enabled`默认为true，即默认**AspectJX**生效

```
aspectjx {
//关闭AspectJX功能
	enabled false
}
```


## 常见问题

* 问：**AspectJX**是否支持`*.aj`文件的编译?

答：
不支持。目前**AspectJX**仅支持annotation的方式，具体可以参考[支持kotlin代码织入的AspectJ Demo](https://github.com/HujiangTechnology/AspectJ-Demo)

* 问：编译时会出现`can't determine superclass of missing type**`及其他编译错误怎么办

答：大部分情况下把出现问题相关的class文件或者库（jar文件）过滤掉就可以搞定了


## 感谢

* 开发**AspectJX**的初衷

 1. 目前的开源库中还没有发现可应用于Android平台的比较好的AOP框架或者工具，虽然[xposed]，[dexposed]非常强大，但基于严重的碎片化现状，兼容问题永远是一座无法逾越的大山。
 2. 目前其他的AspectJ相关插件和框架都不支持AAR或者JAR切入的，对于目前在Android圈很火爆的Kotlin更加无能为力。
 
* 感谢
 1. 该项目的设计参考了大神**JakeWharton**的[Hugo]项目及**uPhyca**的[gradle-android-aspectj-plugin]项目的设计思想，并在它们的基础上扩展支持AAR, JAR及Kotlin的应用。在此感谢JakeWharton和uPhyca.
 2. 感谢热心的**AspectJX**粉丝及其他使用者的积极反馈，你们提供的`PR`以及在[Issues](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx/issues)里提出的问题和答复给大家解决了很多问题，你们都为**AspectJX**贡献了力量
 

### 参考


* [支持kotlin代码织入的AspectJ Demo](https://github.com/HujiangTechnology/AspectJ-Demo)
* [用aspectjx实现的简单、方便、省事的Android M动态权限配置框架](https://github.com/firefly1126/android_permission_aspectjx)


* [AspectJ官网](https://eclipse.org/aspectj/)

* [AspectJ Programming Guide](https://eclipse.org/aspectj/doc/released/progguide/index.html)

* [AspectJ Development Environment Guide](https://eclipse.org/aspectj/doc/released/devguide/index.html)

* [AspectJ NoteBook](https://eclipse.org/aspectj/doc/released/adk15notebook/index.html)


### License


    Copyright 2018 firefly1126, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.gradle_plugin_android_aspectjx
