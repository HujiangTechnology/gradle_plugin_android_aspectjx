package com.hujiang.gradle.plugin.android.aspectjx

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * aspectj weave code logic here
 * @author simon
 * @version 1.0.0
 * @since 2016-04-20
 */
class AspectWork {

    Project project
    public String encoding
    public ArrayList<File> inPath = new ArrayList<File>()
    public ArrayList<File> aspectPath = new ArrayList<File>()
    public ArrayList<File> classPath = new ArrayList<File>()
    public String bootClassPath
    public String sourceCompatibility
    public String targetCompatibility
    public String destinationDir

    protected List<File> source = new ArrayList<File>();

    AspectWork(Project proj) {
        project = proj
    }

    public List<File> getSource() {
        return source;
    }

    public void setSource(List<File> source) {
        this.source = source;
    }

    void doWork() {
        final def log = project.logger

        //http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html
        //
        // -sourceRoots:
        //  Find and build all .java or .aj source files under any directory listed in DirPaths. DirPaths, like classpath, is a single argument containing a list of paths to directories, delimited by the platform- specific classpath delimiter. Required by -incremental.
        // -inPath:
        //  Accept as source bytecode any .class files in the .jar files or directories on Path. The output will include these classes, possibly as woven with any applicable aspects. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        // -classpath:
        //  Specify where to find user class files. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        // -aspectPath:
        //  Weave binary aspects from jar files and directories on path into all sources. The aspects should have been output by the same version of the compiler. When running the output classes, the run classpath should contain all aspectPath entries. Path, like classpath, is a single argument containing a list of paths to jar files, delimited by the platform- specific classpath delimiter.
        // -bootclasspath:
        //  Override location of VM's bootclasspath for purposes of evaluating types when compiling. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
        // -d:
        //  Specify where to place generated .class files. If not specified, Directory defaults to the current working dir.
        // -preserveAllLocals:
        //  Preserve all local variables during code generation (to facilitate debugging).

        def args = [
                "-showWeaveInfo",
                "-encoding", encoding,
                "-source", sourceCompatibility,
                "-target", targetCompatibility,
                "-d", destinationDir,
                "-classpath", classPath.join(File.pathSeparator),
                "-bootclasspath", bootClassPath
//                "-sourceroots", getSourceRoots().join(File.pathSeparator)
        ]
        if (!getInPath().isEmpty()) {
            args << '-inpath'
            args << getInPath().join(File.pathSeparator)
        }
        if (!getAspectPath().isEmpty()) {
            args << '-aspectpath'
            args << getAspectPath().join(File.pathSeparator)
        }

//        println "AspectjCompile:args:" + Arrays.toString(args as String[])

        MessageHandler handler = new MessageHandler(true);
        new Main().run(args as String[], handler);
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    throw new GradleException(message.message, message.thrown)
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break;
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break;
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break;
            }
        }
    }


    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public ArrayList<File> getInPath() {
        return inPath;
    }

    public void setInPath(ArrayList<File> inPath) {
        this.inPath = inPath;
    }

    public ArrayList<File> getAspectPath() {
        return aspectPath;
    }

    public void setAspectPath(ArrayList<File> aspectPath) {
        this.aspectPath = aspectPath;
    }

    public ArrayList<File> getClassPath() {
        return classPath;
    }

    public void setClassPath(ArrayList<File> classPath) {
        this.classPath = classPath;
    }

    public String getBootClassPath() {
        return bootClassPath;
    }

    public void setBootClassPath(String bootClassPath) {
        this.bootClassPath = bootClassPath;
    }

    public String getSourceCompatibility() {
        return sourceCompatibility;
    }

    public void setSourceCompatibility(String sourceCompatibility) {
        this.sourceCompatibility = sourceCompatibility;
    }

    public String getTargetCompatibility() {
        return targetCompatibility;
    }

    public void setTargetCompatibility(String targetCompatibility) {
        this.targetCompatibility = targetCompatibility;
    }

    public String getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(String destinationDir) {
        this.destinationDir = destinationDir;
    }
//
//    File[] getSourceRoots() {
//        def sourceRoots = []
//
//        for (File f : source) {
//            if (f.exists()) {
//                sourceRoots << f
//                println ">>>>>>>${f.path}::${f.exists()}"
//            }
//        }
//
//        return sourceRoots
//    }
}
