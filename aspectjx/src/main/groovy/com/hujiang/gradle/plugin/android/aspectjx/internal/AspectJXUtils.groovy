package com.hujiang.gradle.plugin.android.aspectjx.internal

import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-02-01
 */
class AspectJXUtils {

    static boolean isAspectClass(File classFile) {

        if (isClassFile(classFile)) {
            ClassReader classReader = new ClassReader(FileUtils.readFileToByteArray(classFile))
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
            AspectJClassVisitor aspectJClassVisitor = new AspectJClassVisitor(classWriter)
            classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)

            return aspectJClassVisitor.isAspectClass
        }

        return false
    }

    static FileType fileType(File file) {
        String filePath = file?.getAbsolutePath()

        if (filePath?.toLowerCase().endsWith('.java')) {
            return FileType.JAVA
        } else if (filePath?.toLowerCase().endsWith('.class')) {
            return FileType.CLASS
        } else if (filePath?.toLowerCase().endsWith('.jar')) {
            return FileType.JAR
        } else if (filePath?.toLowerCase().endsWith('.kt')) {
            return FileType.KOTLIN
        } else if (filePath?.toLowerCase().endsWith('.groovy')) {
            return FileType.GROOVY
        } else {
            return FileType.DEFAULT
        }
    }

    static boolean isClassFile(File file) {
        return fileType(file) == FileType.CLASS
    }
}
