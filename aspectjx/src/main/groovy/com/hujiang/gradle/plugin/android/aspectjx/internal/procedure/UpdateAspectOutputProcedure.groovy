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
class UpdateAspectOutputProcedure extends AbsProcedure {
    AJXTaskManager ajxTaskManager

    UpdateAspectOutputProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        ajxTaskManager = new AJXTaskManager(encoding: ajxCache.encoding, ajcArgs: ajxCache.ajcArgs, bootClassPath: ajxCache.bootClassPath,
                            sourceCompatibility: ajxCache.sourceCompatibility, targetCompatibility: ajxCache.targetCompatibility)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~update aspect output")
        ajxTaskManager.aspectPath << variantCache.aspectDir
        ajxTaskManager.classPath << variantCache.includeFileDir
        ajxTaskManager.classPath << variantCache.excludeFileDir

        if (variantCache.incrementalStatus.isAspectChanged || variantCache.incrementalStatus.isIncludeFileChanged) {
            //process class files
            AJXTask ajxTask = new AJXTask(project)
            File outputJar = transformInvocation.getOutputProvider().getContentLocation("include", variantCache.contentTypes,
                    variantCache.scopes, Format.JAR)
            FileUtils.deleteQuietly(outputJar)

            ajxTask.outputJar = outputJar.absolutePath
            ajxTask.inPath << variantCache.includeFileDir

            ajxTaskManager.addTask(ajxTask)
        }

        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                ajxTaskManager.classPath << jarInput.file
                File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                        jarInput.getScopes(), Format.JAR)

                if (!outputJar.getParentFile()?.exists()) {
                    outputJar.getParentFile()?.mkdirs()
                }

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    if (variantCache.incrementalStatus.isAspectChanged) {
                        FileUtils.deleteQuietly(outputJar)

                        AJXTask ajxTask1 = new AJXTask(project)
                        ajxTask1.inPath << jarInput.file

                        ajxTask1.outputJar = outputJar.absolutePath

                        ajxTaskManager.addTask(ajxTask1)
                    } else {
                        if (!outputJar.exists()) {
                            AJXTask ajxTask1 = new AJXTask(project)
                            ajxTask1.inPath << jarInput.file

                            ajxTask1.outputJar = outputJar.absolutePath

                            ajxTaskManager.addTask(ajxTask1)
                        }
                    }
                }
            }
        }

        ajxTaskManager.batchExecute()

        return true
    }
}
