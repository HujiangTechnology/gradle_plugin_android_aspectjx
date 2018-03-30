package com.hujiang.gradle.plugin.android.aspectjx

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.aspectj.weaver.Dump
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.util.TextUtil

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-14
 */
class AspectJXTask implements ITask {

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

    AspectJXTask(Project proj) {
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
        println "aspect core working start ${this}***************"

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

        println "aspect core working end ${this}***************"

        return null
    }
}
