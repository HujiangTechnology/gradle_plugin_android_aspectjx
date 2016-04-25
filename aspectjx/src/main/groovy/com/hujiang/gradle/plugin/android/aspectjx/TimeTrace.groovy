package com.hujiang.gradle.plugin.android.aspectjx

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.gradle.util.Clock

/**
 * trace task execute time
 * @author simon
 * @version 1.0.0
 * @since 2016-04-20
 */
class TimeTrace implements TaskExecutionListener, BuildListener {

    private Clock clock
    private times = []

    @Override
    void buildStarted(Gradle gradle) {
    }

    @Override
    void settingsEvaluated(Settings settings) {

    }

    @Override
    void projectsLoaded(Gradle gradle) {

    }

    @Override
    void projectsEvaluated(Gradle gradle) {

    }

    @Override
    void buildFinished(BuildResult result) {
        println "Task spend time:"
        for (time in times) {
            if (time[0] > 50) {
                printf "%7sms   %s\n", time
            }
        }
    }

    @Override
    void beforeExecute(Task task) {
        clock = new Clock()
    }

    @Override
    void afterExecute(Task task, TaskState state) {
        def ms = clock.timeInMs
        times.add([ms, task.path])
        task.project.logger.warn("${task.path} spend ${ms}ms")
    }
}
