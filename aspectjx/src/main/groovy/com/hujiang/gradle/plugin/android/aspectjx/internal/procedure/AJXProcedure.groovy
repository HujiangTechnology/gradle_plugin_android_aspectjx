/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.hujiang.gradle.plugin.android.aspectjx.AJXConfig
import com.hujiang.gradle.plugin.android.aspectjx.AJXExtension
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.AJXCache
import org.aspectj.weaver.Dump
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-20
 */
class AJXProcedure extends AbsProcedure {

    Project project
    AJXCache ajxCache

    AJXProcedure(Project proj) {
        super(proj, null, null)

        project = proj
        ajxCache = new AJXCache(project)

        System.setProperty("aspectj.multithreaded", "true")

        def configuration = new AJXConfig(project)

        project.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
                ajxCache.encoding = javaCompile.options.encoding
                ajxCache.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                ajxCache.sourceCompatibility = javaCompile.sourceCompatibility
                ajxCache.targetCompatibility = javaCompile.targetCompatibility
            }

            AJXExtension ajxExtension = project.aspectjx
            //当过滤条件发生变化，clean掉编译缓存
            if (ajxCache.isExtensionChanged(ajxExtension)) {
                project.tasks.findByName('preBuild').dependsOn(project.tasks.findByName("clean"))
            }

            ajxCache.putExtensionConfig(ajxExtension)

            ajxCache.ajcArgs = ajxExtension.ajcArgs
        }

        //set aspectj build log output dir
        File logDir = new File(project.buildDir.absolutePath + File.separator + "outputs" + File.separator + "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        Dump.setDumpDirectory(logDir.absolutePath)
    }
}
