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

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.slf4j.LoggerFactory

import java.lang.reflect.Type
import java.util.jar.JarEntry
import java.util.jar.JarFile

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
            return isAspectClass(FileUtils.readFileToByteArray(classFile))
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

        }

        return false
    }

    static FileType fileType(File file) {
        String filePath = file?.getAbsolutePath()

        if (filePath?.toLowerCase()?.endsWith('.java')) {
            return FileType.JAVA
        } else if (filePath?.toLowerCase()?.endsWith('.class')) {
            return FileType.CLASS
        } else if (filePath?.toLowerCase()?.endsWith('.jar')) {
            return FileType.JAR
        } else if (filePath?.toLowerCase()?.endsWith('.kt')) {
            return FileType.KOTLIN
        } else if (filePath?.toLowerCase()?.endsWith('.groovy')) {
            return FileType.GROOVY
        } else {
            return FileType.DEFAULT
        }
    }

    static boolean isClassFile(File file) {
        return fileType(file) == FileType.CLASS
    }

    static boolean isClassFile(String filePath) {
        return filePath?.toLowerCase()?.endsWith('.class')
    }

    static <T> T fromJsonStringThrowEx(String jsonString, Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(jsonString, clazz)
    }

    static <T> T optFromJsonString(String jsonString, Class<T> clazz) {
        try {
            return gson.fromJson(jsonString, clazz)
        } catch (Throwable e) {
            LoggerFactory.getLogger(AJXPlugin).warn("optFromJsonString(${jsonString}, ${clazz}", e)
        }
        return null
    }

    static <T> T fromJsonStringThrowEx(String jsonString, Type typeOfT) throws JsonSyntaxException {
        return gson.fromJson(jsonString, typeOfT)
    }

    static <T> T optFromJsonString(String json, Type typeOfT) {
        try {
            return gson.fromJson(json, typeOfT)
        } catch (JsonSyntaxException var3) {
            LoggerFactory.getLogger(AJXPlugin).warn("optFromJsonString(${json}, ${typeOfT}", var3)
        }
        return null
    }

    static String toJsonStringThrowEx(Object object) throws Exception  {
        return getGson().toJson(object)
    }

    static String optToJsonString(Object object) {
        try {
            return getGson().toJson(object)
        } catch (Throwable var2) {
            LoggerFactory.getLogger(AJXPlugin).warn("optToJsonString(${object}", var2)
        }
        return null
    }

    /**
     * @param transformInvocation
     */
    static void doWorkWithNoAspectj(TransformInvocation transformInvocation) {
        LoggerFactory.getLogger(AJXPlugin).debug("do nothing ~~~~~~~~~~~~~~~~~~~~~~~~")
        if (transformInvocation.incremental) {
            incrementalCopyFiles(transformInvocation)
        } else {
            fullCopyFiles(transformInvocation)
        }
    }

    static void fullCopyFiles(TransformInvocation transformInvocation) {
        transformInvocation.outputProvider.deleteAll()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput->
                File excludeJar = transformInvocation.getOutputProvider().getContentLocation("exclude", dirInput.contentTypes, dirInput.scopes, Format.JAR)
                mergeJar(dirInput.file, excludeJar)
            }

            input.jarInputs.each { JarInput jarInput->
                def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name
                        , jarInput.contentTypes
                        , jarInput.scopes
                        , Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
    }

    static void incrementalCopyFiles(TransformInvocation transformInvocation) {
        transformInvocation.inputs.each {TransformInput input ->
            input.directoryInputs.each {DirectoryInput dirInput ->
                if (dirInput.changedFiles.size() > 0) {
                    File excludeJar = transformInvocation.getOutputProvider().getContentLocation("exclude", dirInput.contentTypes, dirInput.scopes, Format.JAR)
                    mergeJar(dirInput.file, excludeJar)
                }
            }

            input.jarInputs.each {JarInput jarInput ->
                File target = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                switch (jarInput.status) {
                    case Status.REMOVED:
                        FileUtils.forceDelete(target)
                        break
                    case Status.CHANGED:
                        FileUtils.forceDelete(target)
                        FileUtils.copyFile(jarInput.file, target)
                        break
                    case Status.ADDED:
                        FileUtils.copyFile(jarInput.file, target)
                        break
                    default:
                        break
                }
            }
        }
    }

    static boolean isExcludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    static boolean  isIncludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    static boolean isFilterMatched(String str, List<String> filters, FilterPolicy filterPolicy) {
        if(str == null) {
            return false
        }

        if (filters == null || filters.isEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }

        for (String s : filters) {
            if (isContained(str, s)) {
                return true
            }
        }

        return false
    }

    static boolean isContained(String str, String filter) {
        if (str == null) {
            return false
        }

        String filterTmp = filter
        if (str.contains(filterTmp)) {
            return true
        } else {
            if (filterTmp.contains("/")) {
                return str.contains(filterTmp.replace("/", File.separator))
            } else if (filterTmp.contains("\\")) {
                return str.contains(filterTmp.replace("\\", File.separator))
            }
        }

        return false
    }

    static enum FilterPolicy {
        INCLUDE
        , EXCLUDE
    }

    static int countOfFiles(File file) {
        if (file.isFile()) {
            return 1
        } else {
            File[] files = file.listFiles()
            int total = 0
            for (File f : files) {
                total += countOfFiles(f)
            }

            return total
        }
    }

    static void filterJar(JarInput jarInput, VariantCache variantCache, List<String> includes, List<String> excludes) {
        if (includes.isEmpty() && excludes.isEmpty()) {
            //put in cache
            variantCache.addIncludeJar(jarInput.file.absolutePath)
        } else if (includes.isEmpty()) {
            boolean isExclude = false
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement()
                String entryName = jarEntry.getName()
                String tranEntryName = entryName.replace("/", ".").replace("\\", ".")
                if (isExcludeFilterMatched(tranEntryName, excludes)) {
                    isExclude = true
                    break
                }
            }

            jarFile.close()
            if (!isExclude) {
                //put in cache
                variantCache.addIncludeJar(jarInput.file.absolutePath)
            }
        } else if (excludes.isEmpty()) {
            boolean isInclude = false
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement()
                String entryName = jarEntry.getName()
                String tranEntryName = entryName.replace("/", ".").replace("\\", ".")
                if (isIncludeFilterMatched(tranEntryName, includes)) {
                    isInclude = true
                    break
                }
            }

            jarFile.close()
            if (isInclude) {
                //put in cache
                variantCache.addIncludeJar(jarInput.file.absolutePath)
            }
        } else {
            boolean isIncludeMatched = false
            boolean isExcludeMatched = false
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement()
                String entryName = jarEntry.getName()
                String tranEntryName = entryName.replace("/", ".").replace("\\", ".")
                if (isIncludeFilterMatched(tranEntryName, includes)) {
                    isIncludeMatched = true
                }

                if (isExcludeFilterMatched(tranEntryName, excludes)) {
                    isExcludeMatched = true
                }
            }

            jarFile.close()

            if (isIncludeMatched && !isExcludeMatched) {
                //put in cache
                variantCache.addIncludeJar(jarInput.file.absolutePath)
            }
        }
    }

    static void mergeJar(File sourceDir, File targetJar) {
        if (sourceDir == null) {
            throw new IllegalArgumentException("sourceDir should not be null")
        }

        if (targetJar == null) {
            throw new IllegalArgumentException("targetJar should not be null")
        }

        if (!targetJar.parentFile.exists()) {
            FileUtils.forceMkdir(targetJar.getParentFile())
        }

        FileUtils.deleteQuietly(targetJar)

        JarMerger jarMerger = new JarMerger(targetJar)
        try {
            jarMerger.setFilter(new JarMerger.IZipEntryFilter() {
                @Override
                boolean checkEntry(String archivePath) throws JarMerger.IZipEntryFilter.ZipAbortException {
                    return archivePath.endsWith(SdkConstants.DOT_CLASS)
                }
            })

            jarMerger.addFolder(sourceDir)
        } catch (Exception e) {
            LoggerFactory.getLogger(AJXPlugin).warn("mergeJar(${sourceDir}, ${targetJar}", e)
        } finally {
            jarMerger.close()
        }
    }
}

