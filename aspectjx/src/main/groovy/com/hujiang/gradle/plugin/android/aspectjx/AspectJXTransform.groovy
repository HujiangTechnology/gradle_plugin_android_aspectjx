package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.google.common.collect.ImmutableSet
import com.hujiang.gradle.plugin.android.aspectjx.internal.AspectJXUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

import java.util.concurrent.ThreadPoolExecutor

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-12
 */
class AspectJXTransform extends Transform {

    AspectJXTaskManager mAspectJXTaskManager

    AspectJXTransform(Project proj) {
        mAspectJXTaskManager = new AspectJXTaskManager(proj)

        def configuration = new AndroidConfiguration(mAspectJXTaskManager.project)

        proj.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
                mAspectJXTaskManager.encoding = javaCompile.options.encoding
                mAspectJXTaskManager.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                mAspectJXTaskManager.sourceCompatibility = javaCompile.sourceCompatibility
                mAspectJXTaskManager.targetCompatibility = javaCompile.targetCompatibility
            }
        }
    }

    @Override
    String getName() {
        return "aspectjx"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType>of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量编译
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformTask transformTask = (TransformTask)transformInvocation.context

        println ">>>>>>>>>>>>>>>>>>>>>>>>>>${mAspectJXTaskManager}"

        if (transformTask.variantName.contains("AndroidTest")) {
            //stay the same
            transformInvocation.inputs.each {TransformInput input ->
                input.directoryInputs.each { DirectoryInput directoryInput->
                    def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name
                            , directoryInput.contentTypes
                            , directoryInput.scopes
                            , Format.DIRECTORY)
                    FileUtils.copyDirectory(directoryInput.file, dest)
                }

                input.jarInputs.each { JarInput jarInput->
                    def jarName = jarInput.name
                    def dest = transformInvocation.outputProvider.getContentLocation(jarName
                            , jarInput.contentTypes
                            , jarInput.scopes
                            , Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
            return
        }

        //aspectj supports multi thread
        System.setProperty("aspectj.multithreaded", "true")

        if (transformInvocation.incremental) {
            //增量编译

        } else {
            //delete output
            transformInvocation.outputProvider.deleteAll()

            //
            transformInvocation.inputs.each {TransformInput input ->
                AspectJXTask aspectJXTask = new AspectJXTask(mAspectJXTaskManager.project)
                input.directoryInputs.each {DirectoryInput dirInput ->
                    //process dir
                    File outputDir = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.getContentTypes(),
                            dirInput.getScopes(), Format.DIRECTORY)
                    if (!outputDir.exists()) {
                        outputDir.mkdirs()
                    }

                    aspectJXTask.outputDir = outputDir
                    println "outputDir::::${outputDir}"
//                    dirInput.file.eachFileRecurse {File item ->
//                        if (AspectJXUtils.isAspectClass(item)) {
//                            mAspectJXTaskManager.aspectPath << item
//                            println "isASpectClass:::${item.absolutePath}"
//                        }
//                        mAspectJXTaskManager.classPath << item
//                        aspectJXTask.inPath << item
//                    }
                    mAspectJXTaskManager.aspectPath << dirInput.file
                    mAspectJXTaskManager.classPath << dirInput.file
                    aspectJXTask.inPath << dirInput.file

                    mAspectJXTaskManager.addTask(aspectJXTask)
                }

                input.jarInputs.each {JarInput jarInput ->
                    //process jar
                    File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                            jarInput.getScopes(), Format.JAR)
                    if (!outputJar.getParentFile()?.exists()) {
                        outputJar.getParentFile()?.mkdirs()
                    }

                    mAspectJXTaskManager.classPath << jarInput.file
                    AspectJXTask task = new AspectJXTask(mAspectJXTaskManager.project)
                    task.inPath << jarInput.file
                    task.outputJar = outputJar

                    println "jar:::${jarInput.file.absolutePath}"
                    mAspectJXTaskManager.addTask(task)
                }
            }

            //全量编译
            //收集Aspect class
            //过滤class
            //app module / library module 支持
            //多线程执行
            println "AspectJX start working>>>>>>>>>>>>>>>>>>>>>>>"
            mAspectJXTaskManager.batchExecute()
        }

        println "AspectJX end>>>>>>>>>>>>>>>>>>>>>>>"
    }
}
