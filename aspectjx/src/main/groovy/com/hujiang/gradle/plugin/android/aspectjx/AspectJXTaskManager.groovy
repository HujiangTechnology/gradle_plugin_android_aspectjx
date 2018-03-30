package com.hujiang.gradle.plugin.android.aspectjx

import org.aspectj.weaver.Dump
import org.gradle.api.Project
import org.gradle.tooling.internal.consumer.ExecutorServiceFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-14
 */
class AspectJXTaskManager {

    ExecutorService mExecutorService
    List<AspectJXTask> mTasks = new ArrayList<>()

    Project project
    String encoding
    ArrayList<File> aspectPath = new ArrayList<>()
    ArrayList<File> classPath = new ArrayList<>()
    List<String> ajcArgs = new ArrayList<>()
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    AspectJXTaskManager(Project proj) {
        project = proj
        mExecutorService = Executors.newScheduledThreadPool(Runtime.runtime.availableProcessors() + 1)

        File logDir = new File(project.buildDir.absolutePath + File.separator + "outputs" + File.separator + "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        Dump.setDumpDirectory(logDir)
    }


    void addTask(AspectJXTask task) {
        mTasks << task
    }

    void batchExecute() {
        mTasks.each {AspectJXTask task ->
            task.encoding = encoding
            task.aspectPath = aspectPath
            task.classPath = classPath
            task.targetCompatibility = targetCompatibility
            task.sourceCompatibility = sourceCompatibility
            task.bootClassPath = bootClassPath
            task.ajcArgs = ajcArgs
//            task.call()
        }


        mExecutorService.invokeAll(mTasks)

        mTasks.clear()
    }
}
