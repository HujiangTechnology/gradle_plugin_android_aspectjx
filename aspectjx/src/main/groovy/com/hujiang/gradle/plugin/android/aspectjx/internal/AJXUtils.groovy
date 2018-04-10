package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.lang.reflect.Type

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-02-01
 */
class AJXUtils {

    static Gson gson = new GsonBuilder().create()

    static boolean isAspectClass(File classFile) {

        if (isClassFile(classFile)) {
            try {
                ClassReader classReader = new ClassReader(FileUtils.readFileToByteArray(classFile))
                ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
                AspectJClassVisitor aspectJClassVisitor = new AspectJClassVisitor(classWriter)
                classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)

                return aspectJClassVisitor.isAspectClass
            } catch (Exception e) {
                println "${e.toString()}"
            }
        }

        return false
    }

    static boolean isAspectClass(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false
        }

        try {
            ClassReader classReader = new ClassReader(bytes)
            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
            AspectJClassVisitor aspectJClassVisitor = new AspectJClassVisitor(classWriter)
            classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)

            return aspectJClassVisitor.isAspectClass
        } catch (Exception e) {
            println "${e.toString()}"
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

    static boolean isClassFile(String filePath) {
        return filePath?.toLowerCase().endsWith('.class')
    }

    public static <T> T fromJsonStringThrowEx(String jsonString, Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(jsonString, clazz)
    }

    public static <T> T optFromJsonString(String jsonString, Class<T> clazz) {
        try {
            return gson.fromJson(jsonString, clazz)
        } catch (Throwable e) {
            e.printStackTrace()
        }
    }

    public static <T> T fromJsonStringThrowEx(String jsonString, Type typeOfT) throws JsonSyntaxException {
        return gson.fromJson(jsonString, typeOfT)
    }

    public static <T> T optFromJsonString(String json, Type typeOfT) {
        try {
            return gson.fromJson(json, typeOfT)
        } catch (JsonSyntaxException var3) {
            var3.printStackTrace()
        }
    }

    public static String toJsonStringThrowEx(Object object) throws Exception  {
        return getGson().toJson(object)
    }

    public static String optToJsonString(Object object) {
        try {
            return getGson().toJson(object)
        } catch (Throwable var2) {
            var2.printStackTrace()
        }
    }
}

