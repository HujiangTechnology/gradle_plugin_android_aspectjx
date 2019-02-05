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

import com.android.build.api.transform.*
import com.google.common.io.ByteStreams
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class UpdateAspectFilesProcedure extends AbsProcedure {
    UpdateAspectFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~update aspect files")
        //update aspect files
        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.inputs.each { TransformInput input->
            input.directoryInputs.each { DirectoryInput dirInput->
                taskScheduler.addTask(new ITask() {
                    @Override
                    Object call() throws Exception {
                        dirInput.changedFiles.each { File file, Status status ->
                            if (AJXUtils.isAspectClass(file)) {
                                project.logger.debug("~~~~~~~~~~~collect aspect file from Dir:${file.absolutePath}")
                                variantCache.incrementalStatus.isAspectChanged = true
                                String path = file.absolutePath
                                String subPath = path.substring(dirInput.file.absolutePath.length())
                                File cacheFile = new File(variantCache.aspectPath + subPath)

                                switch (status) {
                                    case Status.REMOVED:
                                        FileUtils.deleteQuietly(cacheFile)
                                        break
                                    case Status.CHANGED:
                                        FileUtils.deleteQuietly(cacheFile)
                                        variantCache.add(file, cacheFile)
                                        break
                                    case Status.ADDED:
                                        variantCache.add(file, cacheFile)
                                        break
                                    default:
                                        break
                                }
                            }
                        }

                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput->
                if (jarInput.status != Status.NOTCHANGED) {
                    taskScheduler.addTask(new ITask() {
                        @Override
                        Object call() throws Exception {
                            JarFile jarFile = new JarFile(jarInput.file)
                            Enumeration<JarEntry> entries = jarFile.entries()
                            while (entries.hasMoreElements()) {
                                JarEntry jarEntry = entries.nextElement()
                                String entryName = jarEntry.getName()
                                if (!jarEntry.isDirectory() && AJXUtils.isClassFile(entryName)) {
                                    byte[] bytes = ByteStreams.toByteArray(jarFile.getInputStream(jarEntry))
                                    File cacheFile = new File(variantCache.aspectPath + File.separator + entryName)
                                    if (AJXUtils.isAspectClass(bytes)) {
                                        project.logger.debug("~~~~~~~~~~~~~~~~~collect aspect file from JAR:${entryName}")
                                        variantCache.incrementalStatus.isAspectChanged = true
                                        if (jarInput.status == Status.REMOVED) {
                                            FileUtils.deleteQuietly(cacheFile)
                                        } else if (jarInput.status == Status.CHANGED) {
                                            FileUtils.deleteQuietly(cacheFile)
                                            variantCache.add(bytes, cacheFile)
                                        } else if (jarInput.status == Status.ADDED) {
                                            variantCache.add(bytes, cacheFile)
                                        }
                                    }
                                }
                            }

                            jarFile.close()

                            return null
                        }
                    })
                }
            }
        }

        taskScheduler.execute()

        if (AJXUtils.countOfFiles(variantCache.aspectDir) == 0) {
            //do work with no aspectj
            AJXUtils.fullCopyFiles(transformInvocation)
            return false
        }

        return true
    }
}
