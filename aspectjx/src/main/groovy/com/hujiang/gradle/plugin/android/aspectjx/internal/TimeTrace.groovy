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
package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

/**
 * trace task execute time
 * @author simon
 * @version 1.0.0
 * @since 2016-04-20
 */
class TimeTrace implements TaskExecutionListener, BuildListener {

    private clocks = new ConcurrentHashMap()
    private times = []
    private static final DISPLAY_TIME_THRESHOLD = 50

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
        LoggerFactory.getLogger(AJXPlugin).debug("Tasks spend time > ${DISPLAY_TIME_THRESHOLD}ms:")

        times.sort { lhs, rhs -> -(lhs[0] - rhs[0]) }
                .grep { it[0] > DISPLAY_TIME_THRESHOLD }
                .each { time -> printf "%14s   %s\n", formatTime(time[0]), time[1] }
    }

    @Override
    void beforeExecute(Task task) {
        clocks[task.path] = new Clock(System.currentTimeMillis())
    }

    @Override
    void afterExecute(Task task, TaskState state) {
        clocks.remove(task.path)?.with { clock ->
            def ms = clock.timeInMs
            times.add([ms, task.path])
            task.project.logger.warn("${task.path} spend ${ms}ms")
        }
    }

    static def formatTime(ms) {
        def sec = ms.intdiv(1000)
        def min = sec.intdiv(60)
        sec %= 60
        ms = (ms % 1000).intdiv(10)
        return String.format("%02d:%02d.%02d", min, sec, ms)
    }
}
