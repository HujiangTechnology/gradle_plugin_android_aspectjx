package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.google.common.collect.ImmutableSet
import com.google.common.io.ByteStreams
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.BatchTaskScheduler
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-12
 */
class AJXTransform extends Transform {

    Project project
    AJXTaskManager mAJXTaskManager

    AJXTransform(Project proj) {
        project = proj
        mAJXTaskManager = new AJXTaskManager(proj)

        def configuration = new AJXConfig(mAJXTaskManager.project)

        proj.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ? variant.javaCompiler : variant.javaCompile
                mAJXTaskManager.encoding = javaCompile.options.encoding
                mAJXTaskManager.bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                mAJXTaskManager.sourceCompatibility = javaCompile.sourceCompatibility
                mAJXTaskManager.targetCompatibility = javaCompile.targetCompatibility
            }
        }
    }

    @Override
    String getName() {
        return "ajx"
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

        mAJXTaskManager.ajcArgs = project.aspectjx.ajcArgs

        List<String> includes = project.aspectjx.includes
        List<String> excludes = project.aspectjx.excludes
        boolean isExcludeAll = false

        for (String filter : excludes) {
            if (filter == "*" || filter == "**") {
                isExcludeAll = true
                break
            }
        }

        if (!project.aspectjx.enabled || isExcludeAll) {
            doNothing(transformInvocation)
            return
        }

        TransformTask transformTask = (TransformTask)transformInvocation.context
        AJXCache ajxCache = new AJXCache(mAJXTaskManager.project, transformTask.variantName)

        println ">>>>>>>>>>>>>>>>>>>>>>>>>> aspectjx transform starting>>>>>>>>>>>>>>>>>>>>>"

//        if (transformTask.variantName.contains("AndroidTest")) {
//            //stay the same
//            transformInvocation.inputs.each {TransformInput input ->
//                input.directoryInputs.each { DirectoryInput directoryInput->
//                    def dest = transformInvocation.outputProvider.getContentLocation(directoryInput.name
//                            , directoryInput.contentTypes
//                            , directoryInput.scopes
//                            , Format.DIRECTORY)
//                    FileUtils.copyDirectory(directoryInput.file, dest)
//                }
//
//                input.jarInputs.each { JarInput jarInput->
//                    def jarName = jarInput.name
//                    def dest = transformInvocation.outputProvider.getContentLocation(jarName
//                            , jarInput.contentTypes
//                            , jarInput.scopes
//                            , Format.JAR)
//                    FileUtils.copyFile(jarInput.file, dest)
//                }
//            }
//            return
//        }

//        procedure
//        transaction

        //supports multi thread
        System.setProperty("aspectj.multithreaded", "true")

        if (transformInvocation.incremental) {
            //增量编译

        } else {
            //delete output
            transformInvocation.outputProvider.deleteAll()
            ajxCache.reset()


            //////////
            //缓存aspect文件
//            BatchTaskScheduler batchTaskScheduler = new BatchTaskScheduler()

//            transformInvocation.inputs.each {TransformInput input ->
//                input.directoryInputs.each {DirectoryInput dirInput ->
                    //collect aspect file
//                    batchTaskScheduler.addTask(new ITask() {
//                        @Override
//                        Object call() throws Exception {
//                            dirInput.file.eachFileRecurse {File item ->
//                                println "check aspect class::${item.absolutePath}"
//                                if (AJXUtils.isAspectClass(item)) {
//                                    println "collect aspect file:${item.absolutePath}"
//                                    String path = item.absolutePath
//                                    String subPath = path.substring(dirInput.file.absolutePath.length())
//                                    File cacheFile = new File(ajxCache.aspectPath + subPath)
//                                    ajxCache.add(item, cacheFile)
//                                }
//                            }

//                            return null
//                        }
//                    })
//                }

//                input.jarInputs.each {JarInput jarInput ->
                    //collect aspect file
//                    batchTaskScheduler.addTask(new ITask() {
//                        @Override
//                        Object call() throws Exception {
//                            JarFile jarFile = new JarFile(jarInput.file)
//                            Enumeration<JarEntry> entries = jarFile.entries()
//                            while (entries.hasMoreElements()) {
//                                JarEntry jarEntry = entries.nextElement()
//                                String entryName = jarEntry.getName()
//                                if (!jarEntry.isDirectory() && AJXUtils.isClassFile(entryName)) {
//                                    byte[] bytes = ByteStreams.toByteArray(jarFile.getInputStream(jarEntry))
//                                    File cacheFile = new File(ajxCache.aspectPath + File.separator + entryName)
//                                    println "check aspect file::${entryName}"
//                                    if (AJXUtils.isAspectClass(bytes)) {
//                                        println "collect aspect file:${entryName}"
//                                        ajxCache.add(bytes, cacheFile)
//                                    }
//                                }
//                            }
//
//                            jarFile.close()

//                            return null
//                        }
//                    })
//                }
//            }

            println "collect aspect files start~~~~~~~~~~~~~~~~~~~~~"
//            batchTaskScheduler.execute()
            println "collect aspect files end~~~~~~~~~~~~~~~~~~~~~"

//            if (countOfFiles(ajxCache.aspectDir) == 0) {
//                doNothing(transformInvocation)
//                return
//            }

            //过滤规则
            /**
             * "*" 所有class文件和jar
             * "**" 所有class文件和jar
             * "com.hujiang" 过滤 含"com.hujiang"的文件和jar
             */

            println "process filter regulations ~~~~~~~~~~~~~~~~~~~~~~~"
//            boolean  isIncludeAll = false
//            for (String filter : includes) {
//                if (filter == "*" || filter == "**") {
//                    isIncludeAll = true
//                    break
//                }
//            }
//
//            if (isIncludeAll) {
//                includes.clear()
//            }
//
//            def contentTypes = null
//            def scopes = null
//            transformInvocation.inputs.each { TransformInput input ->
//                input.directoryInputs.each { DirectoryInput dirInput ->
//                    contentTypes = dirInput.contentTypes
//                    scopes = dirInput.scopes
//                    dirInput.file.eachFileRecurse {File item ->
//                        if (AJXUtils.isClassFile(item)) {
//                            String path = item.absolutePath
//                            String subPath = path.substring(dirInput.file.absolutePath.length())
//
//                            String transPath = subPath.replace(File.separator, ".")
//
//                            boolean isInclude = isIncludeFilterMatched(transPath, includes) && !isExcludeFilterMatched(transPath, excludes)
//                            ajxCache.add(item, new File((isInclude ? ajxCache.includeFilePath : ajxCache.excludeFilePath) + subPath))
//                        }
//                    }
//                }
//
//                input.jarInputs.each { JarInput jarInput ->
//                    if (includes.isEmpty() && excludes.isEmpty()) {
//                        //put in cache
//                        ajxCache.addIncludeJar(jarInput.file.absolutePath)
//                    } else if (includes.isEmpty()) {
//                        boolean isExclude = false
//                        JarFile jarFile = new JarFile(jarInput.file)
//                        Enumeration<JarEntry> entries = jarFile.entries()
//                        while (entries.hasMoreElements()) {
//                            JarEntry jarEntry = entries.nextElement()
//                            String entryName = jarEntry.getName()
//                            String tranEntryName = entryName.replace(File.separator, ".")
//                            if (isExcludeFilterMatched(tranEntryName, excludes)) {
//                                isExclude = true
//                                break
//                            }
//                        }
//
//                        jarFile.close()
//                        if (!isExclude) {
//                            //put in cache
//                            ajxCache.addIncludeJar(jarInput.file.absolutePath)
//                        }
//                    } else if (excludes.isEmpty()) {
//                        boolean isInclude = false
//                        JarFile jarFile = new JarFile(jarInput.file)
//                        Enumeration<JarEntry> entries = jarFile.entries()
//                        while (entries.hasMoreElements()) {
//                            JarEntry jarEntry = entries.nextElement()
//                            String entryName = jarEntry.getName()
//                            String tranEntryName = entryName.replace(File.separator, ".")
//                            if (isIncludeFilterMatched(tranEntryName, includes)) {
//                                isInclude = true
//                                break
//                            }
//                        }
//
//                        jarFile.close()
//                        if (isInclude) {
//                            //put in cache
//                            ajxCache.addIncludeJar(jarInput.file.absolutePath)
//                        }
//                    } else {
//                        boolean isIncludeMatched = false
//                        boolean isExcludeMatched = false
//                        JarFile jarFile = new JarFile(jarInput.file)
//                        Enumeration<JarEntry> entries = jarFile.entries()
//                        while (entries.hasMoreElements()) {
//                            JarEntry jarEntry = entries.nextElement()
//                            String entryName = jarEntry.getName()
//                            String tranEntryName = entryName.replace(File.separator, ".")
//                            if (isIncludeFilterMatched(tranEntryName, includes)) {
//                                isIncludeMatched = true
//                            }
//
//                            if (isExcludeFilterMatched(tranEntryName, excludes)) {
//                                isExcludeMatched = true
//                            }
//                        }
//
//                        jarFile.close()
//
//                        if (isIncludeMatched && !isExcludeMatched) {
//                            //put in cache
//                            ajxCache.addIncludeJar(jarInput.file.absolutePath)
//                        }
//                    }
//                }
//            }
//
//            ajxCache.commit()

            //do aspectj real work
            println "do aspectj real work ~~~~~~~~~~~~~~~~~~~~~~~~"
//            mAJXTaskManager.aspectPath << ajxCache.aspectDir
//            mAJXTaskManager.classPath << ajxCache.includeFileDir
//            mAJXTaskManager.classPath << ajxCache.excludeFileDir

            //process class files
//            AJXTask ajxTask = new AJXTask(project)
//            File outputDir = transformInvocation.getOutputProvider().getContentLocation("include", contentTypes, scopes, Format.DIRECTORY)
//            if (!outputDir.exists()) {
//                outputDir.mkdirs()
//            }
//            ajxTask.outputDir = outputDir.absolutePath
//            ajxTask.inPath << ajxCache.includeFileDir
//
//            mAJXTaskManager.addTask(ajxTask)

            //
            transformInvocation.inputs.each { TransformInput input ->
                input.directoryInputs.each {DirectoryInput dirInput ->
                    mAJXTaskManager.aspectPath << dirInput
                    mAJXTaskManager.classPath << dirInput

                    File outputDir = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                    if (!outputDir.exists()) {
                        outputDir.mkdirs()
                    }

                    AJXTask task = new AJXTask()
                    task.outputDir << outputDir.absolutePath
                    task.inPath << dirInput.file

                    mAJXTaskManager.addTask(task)
                }

                input.jarInputs.each {JarInput jarInput ->
                    mAJXTaskManager.classPath << jarInput.file

                    if (ajxCache.isIncludeJar(jarInput.file.absolutePath)) {
                        AJXTask ajxTask1 = new AJXTask(project)
                        ajxTask1.inPath << jarInput.file

                        File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                                jarInput.getScopes(), Format.JAR)
                        if (!outputJar.getParentFile()?.exists()) {
                            outputJar.getParentFile()?.mkdirs()
                        }

                        ajxTask1.outputJar = outputJar.absolutePath

                        mAJXTaskManager.addTask(ajxTask1)
                    }
                }
            }

            println "AspectJX start working>>>>>>>>>>>>>>>>>>>>>>>"
            mAJXTaskManager.executeTasks()
            println "AspectJX end>>>>>>>>>>>>>>>>>>>>>>>"

            //process excluded files and jars
            println "process excluded files and jars ~~~~~~~~~~~~~~~~~~"
//            File excludeOutput = transformInvocation.getOutputProvider().getContentLocation("exclude", contentTypes, scopes, Format.DIRECTORY)
//            if (!excludeOutput.exists()) {
//                excludeOutput.mkdirs()
//            }
//
//            FileUtils.copyDirectory(ajxCache.excludeFileDir, excludeOutput)
//
//            transformInvocation.inputs.each {TransformInput input ->
//                input.jarInputs.each {JarInput jarInput ->
//                    if (!ajxCache.isIncludeJar(jarInput.file.absolutePath)) {
//                        def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name
//                                , jarInput.contentTypes
//                                , jarInput.scopes
//                                , Format.JAR)
//                        FileUtils.copyFile(jarInput.file, dest)
//                    }
//                }
            }


            //
//            transformInvocation.inputs.each {TransformInput input ->
//                AJXTask aspectJXTask = new AJXTask(mAJXTaskManager.project)
//                input.directoryInputs.each {DirectoryInput dirInput ->
//                    //outputdir
//                    File outputDir = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.getContentTypes(),
//                            dirInput.getScopes(), Format.DIRECTORY)
//                    if (!outputDir.exists()) {
//                        outputDir.mkdirs()
//                    }
//                    //output dir
//                    aspectJXTask.outputDir = outputDir
//                    println "outputDir::::${outputDir}"
//
//                    //aspect path
//                    mAJXTaskManager.aspectPath << dirInput.file
//                    //class path
//                    mAJXTaskManager.classPath << dirInput.file
//
//                    //inpath
//                    aspectJXTask.inPath << dirInput.file
//
//                    //add task
//                    mAJXTaskManager.addTask(aspectJXTask)
//                }
//
//                ///////
//                input.jarInputs.each {JarInput jarInput ->
//                    //process jar
//
//
//                    File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
//                            jarInput.getScopes(), Format.JAR)
//                    if (!outputJar.getParentFile()?.exists()) {
//                        outputJar.getParentFile()?.mkdirs()
//                    }
//
//                    mAJXTaskManager.classPath << jarInput.file
//                    AJXTask task = new AJXTask(mAJXTaskManager.project)
//                    task.inPath << jarInput.file
//                    task.outputJar = outputJar
//
//                    println "jar:::${jarInput.file.absolutePath}"
//                    mAJXTaskManager.addTask(task)
//                }
//            }

            //全量编译
            //收集Aspect class
            //过滤class
            //app module / library module 支持
            //多线程执行
//            mAJXTaskManager.executeTasks()
//        }

        println "work done~~~~~~~~~~~~~~~~~~"
    }

    void doNothing(TransformInvocation transformInvocation) {
        println "doNothing ~~~~~~~~~~~~~~~~~~~~~~~~"
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
    }

    boolean isExcludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    boolean  isIncludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    boolean isFilterMatched(String str, List<String> filters, FilterPolicy filterPolicy) {
        if(str == null) {
            return false
        }

        if (filters == null || filters.isEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }

        for (String s : filters) {
            if (isContained(str, s)) {
                return true
            }
        }

        return false
    }

    boolean isContained(String str, String filter) {
        if (str == null) {
            return false
        }

        String filterTmp = filter
        if (str.contains(filterTmp)) {
            return true
        } else {
            if (filterTmp.contains("/")) {
                return str.contains(filterTmp.replace("/", File.separator))
            } else if (filterTmp.contains("\\")) {
                return str.contains(filterTmp.replace("\\", File.separator))
            }
        }

        return false
    }

    enum FilterPolicy {
        INCLUDE
        , EXCLUDE
    }

    int countOfFiles(File file) {
        if (file.isFile()) {
            return 1
        } else {
            File[] files = file.listFiles()
            int total = 0
            for (File f : files) {
                total += countOfFiles(f)
            }

            return total
        }
    }
}
