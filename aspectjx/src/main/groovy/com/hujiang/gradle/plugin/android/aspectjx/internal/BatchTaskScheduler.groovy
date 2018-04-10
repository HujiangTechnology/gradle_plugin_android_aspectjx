package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.hujiang.gradle.plugin.android.aspectjx.AJXTask
import com.hujiang.gradle.plugin.android.aspectjx.ITask

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-04
 */
class BatchTaskScheduler {

    ExecutorService executorService
    List< ? extends ITask> tasks = new ArrayList<>()

    BatchTaskScheduler() {
        executorService = Executors.newScheduledThreadPool(Runtime.runtime.availableProcessors() + 1)
    }

    public <T extends ITask> void addTask(T task) {
        tasks << task
    }

    void execute() {
        executorService.invokeAll(tasks)
        tasks.clear()
    }

}
