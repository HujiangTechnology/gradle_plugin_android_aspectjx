package com.hujiang.gradle.plugin.android.aspectjx

import com.hujiang.gradle.plugin.android.aspectjx.internal.BatchTaskScheduler
import org.aspectj.weaver.Dump
import org.gradle.api.Project

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-14
 */
class AJXTaskManager {

    Project project
    String encoding
    ArrayList<File> aspectPath = new ArrayList<>()
    ArrayList<File> classPath = new ArrayList<>()
    List<String> ajcArgs = new ArrayList<>()
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    BatchTaskScheduler batchTaskScheduler = new BatchTaskScheduler()

    AJXTaskManager(Project proj) {
        project = proj

        //set dump dir
        File logDir = new File(project.buildDir.absolutePath + File.separator + "outputs" + File.separator + "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        Dump.setDumpDirectory(logDir.absolutePath)
    }


    void addTask(AJXTask task) {
        batchTaskScheduler.tasks << task
    }

    void executeTasks() {
        batchTaskScheduler.tasks.each { AJXTask task ->
            task.encoding = encoding
            task.aspectPath = aspectPath
            task.classPath = classPath
            task.targetCompatibility = targetCompatibility
            task.sourceCompatibility = sourceCompatibility
            task.bootClassPath = bootClassPath
            task.ajcArgs = ajcArgs
        }

        batchTaskScheduler.execute()
    }
}
