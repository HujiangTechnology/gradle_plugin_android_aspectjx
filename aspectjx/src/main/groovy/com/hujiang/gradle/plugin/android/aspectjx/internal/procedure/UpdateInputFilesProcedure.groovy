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
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class UpdateInputFilesProcedure extends AbsProcedure {

    UpdateInputFilesProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~update input files")
        BatchTaskScheduler taskScheduler = new BatchTaskScheduler()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                taskScheduler.addTask(new ITask() {
                    @Override
                    Object call() throws Exception {
                        dirInput.changedFiles.each { File file, Status status ->
                            project.logger.debug("~~~~~~~~~~~~~~~~changed file::${status.name()}::${file.absolutePath}")

                            variantCache.includeFileContentTypes = dirInput.contentTypes
                            variantCache.includeFileScopes = dirInput.scopes

                            String path = file.absolutePath
                            String subPath = path.substring(dirInput.file.absolutePath.length())
                            String transPath = subPath.replace(File.separator, ".")

                            boolean isInclude = AJXUtils.isIncludeFilterMatched(transPath, ajxExtensionConfig.includes) \
                                        && !AJXUtils.isExcludeFilterMatched(transPath, ajxExtensionConfig.excludes)

                            if (!variantCache.incrementalStatus.isIncludeFileChanged && isInclude) {
                                variantCache.incrementalStatus.isIncludeFileChanged = isInclude
                            }

                            if (!variantCache.incrementalStatus.isExcludeFileChanged && !isInclude) {
                                variantCache.incrementalStatus.isExcludeFileChanged = !isInclude
                            }

                            File target = new File((isInclude ? variantCache.includeFilePath : variantCache.excludeFilePath) + subPath)
                            switch (status) {
                                case Status.REMOVED:
                                    FileUtils.deleteQuietly(target)
                                    break
                                case Status.CHANGED:
                                    FileUtils.deleteQuietly(target)
                                    variantCache.add(file, target)
                                    break
                                case Status.ADDED:
                                    variantCache.add(file, target)
                                    break
                                default:
                                    break
                            }
                        }
                        //如果include files 发生变化，则删除include输出jar
                        if (variantCache.incrementalStatus.isIncludeFileChanged) {
                            File includeOutputJar = transformInvocation.outputProvider.getContentLocation("include", variantCache.contentTypes,
                                    variantCache.scopes, Format.JAR)
                            FileUtils.deleteQuietly(includeOutputJar)
                        }

                        //如果exclude files发生变化，则重新生成exclude jar到输出目录
                        if (variantCache.incrementalStatus.isExcludeFileChanged) {
                            File excludeOutputJar = transformInvocation.outputProvider.getContentLocation("exclude", variantCache.contentTypes,
                                    variantCache.scopes, Format.JAR)
                            FileUtils.deleteQuietly(excludeOutputJar)
                            AJXUtils.mergeJar(variantCache.excludeFileDir, excludeOutputJar)
                        }

                        return null
                    }
                })
            }

            input.jarInputs.each { JarInput jarInput ->
                if (jarInput.status != Status.NOTCHANGED) {
                    taskScheduler.addTask(new ITask() {
                        @Override
                        Object call() throws Exception {
                            project.logger.debug("~~~~~~~changed file::${jarInput.status.name()}::${jarInput.file.absolutePath}")

                            String filePath = jarInput.file.absolutePath
                            File outputJar = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                            if (jarInput.status == Status.REMOVED) {
                                variantCache.removeIncludeJar(filePath)
                                FileUtils.deleteQuietly(outputJar)
                            } else if (jarInput.status == Status.ADDED) {
                                AJXUtils.filterJar(jarInput, variantCache, ajxExtensionConfig.includes, ajxExtensionConfig.excludes)
                            } else if (jarInput.status == Status.CHANGED) {
                                FileUtils.deleteQuietly(outputJar)
                            }

                            //将不需要做AOP处理的文件原样copy到输出目录
                            if (!variantCache.isIncludeJar(filePath)) {
                                FileUtils.copyFile(jarInput.file, outputJar)
                            }
                            return null
                        }
                    })
                }
            }
        }

        taskScheduler.execute()

        variantCache.commitIncludeJarConfig()

        return true
    }
}
