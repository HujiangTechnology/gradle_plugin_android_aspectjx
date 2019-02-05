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

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXTask
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXTaskManager
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class DoAspectWorkProcedure extends AbsProcedure {
    AJXTaskManager ajxTaskManager

    DoAspectWorkProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        ajxTaskManager = new AJXTaskManager(encoding: ajxCache.encoding, ajcArgs: ajxCache.ajcArgs, bootClassPath: ajxCache.bootClassPath,
                sourceCompatibility: ajxCache.sourceCompatibility, targetCompatibility: ajxCache.targetCompatibility)
    }

    @Override
    boolean doWorkContinuously() {
        //do aspectj real work
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~do aspectj real work")
        ajxTaskManager.aspectPath << variantCache.aspectDir
        ajxTaskManager.classPath << variantCache.includeFileDir
        ajxTaskManager.classPath << variantCache.excludeFileDir

        //process class files
        AJXTask ajxTask = new AJXTask(project)
        File includeJar = transformInvocation.getOutputProvider().getContentLocation("include", variantCache.contentTypes,
                variantCache.scopes, Format.JAR)

        if (!includeJar.parentFile.exists()) {
            FileUtils.forceMkdir(includeJar.getParentFile())
        }

        FileUtils.deleteQuietly(includeJar)

        ajxTask.outputJar = includeJar.absolutePath
        ajxTask.inPath << variantCache.includeFileDir
        ajxTaskManager.addTask(ajxTask)

        //process jar files
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                ajxTaskManager.classPath << jarInput.file

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    AJXTask ajxTask1 = new AJXTask(project)
                    ajxTask1.inPath << jarInput.file

                    File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                            jarInput.getScopes(), Format.JAR)
                    if (!outputJar.getParentFile()?.exists()) {
                        outputJar.getParentFile()?.mkdirs()
                    }

                    ajxTask1.outputJar = outputJar.absolutePath

                    ajxTaskManager.addTask(ajxTask1)
                }
            }
        }

        ajxTaskManager.batchExecute()

        return true
    }
}
