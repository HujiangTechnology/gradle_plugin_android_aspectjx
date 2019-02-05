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

import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-14
 */
class AJXTask implements ITask {

    Project project
    String encoding
    ArrayList<File> inPath = new ArrayList<>()
    ArrayList<File> aspectPath = new ArrayList<>()
    ArrayList<File> classPath = new ArrayList<>()
    List<String> ajcArgs = new ArrayList<>()
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility
    String outputDir
    String outputJar

    AJXTask(Project proj) {
        project = proj
    }

    @Override
    Object call() throws Exception {
        final def log = project.logger
        def args = [
                "-showWeaveInfo",
                "-encoding", encoding,
                "-source", sourceCompatibility,
                "-target", targetCompatibility,
                "-classpath", classPath.join(File.pathSeparator),
                "-bootclasspath", bootClassPath
        ]

        if (!getInPath().isEmpty()) {
            args << '-inpath'
            args << getInPath().join(File.pathSeparator)
        }
        if (!getAspectPath().isEmpty()) {
            args << '-aspectpath'
            args << getAspectPath().join(File.pathSeparator)
        }

        if (outputDir != null && !outputDir.isEmpty()) {
            args << '-d'
            args << outputDir
        }

        if (outputJar != null && !outputJar.isEmpty()) {
            args << '-outjar'
            args << outputJar
        }

        if(ajcArgs != null && !ajcArgs.isEmpty()) {
            if (!ajcArgs.contains('-Xlint')) {
                args.add('-Xlint:ignore')
            }
            if (!ajcArgs.contains('-warn')) {
                args.add('-warn:none')
            }

            args.addAll(ajcArgs)
        } else {
            args.add('-Xlint:ignore')
            args.add('-warn:none')
        }

        inPath.each {File file ->
            project.logger.debug("~~~~~~~~~~~~~input file: ${file.absolutePath}")
        }

        MessageHandler handler = new MessageHandler(true)
        Main m = new Main()
        m.run(args as String[], handler)
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    throw new GradleException(message.message, message.thrown)
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break
            }
        }

        return null
    }
}
