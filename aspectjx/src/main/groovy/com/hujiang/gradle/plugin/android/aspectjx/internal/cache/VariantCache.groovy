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
package com.hujiang.gradle.plugin.android.aspectjx.internal.cache

import com.android.build.api.transform.QualifiedContent
import com.android.builder.model.AndroidProject
import com.google.common.collect.ImmutableSet
import com.google.gson.reflect.TypeToken
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.model.JarInfo
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-13
 */
class VariantCache {


    Project project
    AJXCache ajxCache
    String variantName
    String cachePath
    String aspectPath
    String includeFilePath
    String excludeFilePath
    String includeJarConfigPath

    IncrementalStatus incrementalStatus
    Set<QualifiedContent.ContentType> includeFileContentTypes
    Set<QualifiedContent.Scope> includeFileScopes
    Set<QualifiedContent.ContentType> contentTypes = ImmutableSet.<QualifiedContent.ContentType>of(QualifiedContent.DefaultContentType.CLASSES)
    Set<QualifiedContent.Scope> scopes = ImmutableSet.<QualifiedContent.Scope>of(QualifiedContent.Scope.EXTERNAL_LIBRARIES)

    Map<String, JarInfo> includeJarInfos = new ConcurrentHashMap<>()

    VariantCache(Project proj, AJXCache cache, String variantName) {
        this.project = proj
        this.variantName = variantName
        this.ajxCache = cache
        this.ajxCache.put(variantName, this)

        incrementalStatus = new IncrementalStatus()

        init()
    }

    private void init() {
        cachePath = project.buildDir.absolutePath + File.separator + AndroidProject.FD_INTERMEDIATES + "/ajx/" + variantName
        aspectPath = cachePath + File.separator + "aspecs"
        includeFilePath = cachePath + File.separator + "includefiles"
        excludeFilePath = cachePath + File.separator + "excludefiles"
        includeJarConfigPath = cachePath + File.separator + "includejars.json"

        if (!aspectDir.exists()) {
            aspectDir.mkdirs()
        }

        if (!includeFileDir.exists()) {
            includeFileDir.mkdirs()
        }

        if (!excludeFileDir.exists()) {
            excludeFileDir.mkdirs()
        }

        if (includeJarConfig.exists()) {
            List<JarInfo> jarInfoList = AJXUtils.optFromJsonString(FileUtils.readFileToString(includeJarConfig), new TypeToken<List<JarInfo>>(){}.getType())

            if (jarInfoList != null) {
                jarInfoList.each {JarInfo jarInfo->
                    includeJarInfos.put(jarInfo.path, jarInfo)
                }
            }
        }
    }

    File getCacheDir() {
        return new File(cachePath)
    }

    File getAspectDir() {
        return new File(aspectPath)
    }

    File getIncludeFileDir() {
        return new File(includeFilePath)
    }

    File getExcludeFileDir() {
        return new File(excludeFilePath)
    }

    File getIncludeJarConfig() {
        return new File(includeJarConfigPath)
    }

    void add(File sourceFile, File cacheFile) {
        if (sourceFile == null || cacheFile == null) {
            return
        }

        byte[] bytes = FileUtils.readFileToByteArray(sourceFile)
        add(bytes, cacheFile)
    }

    void add(byte[] classBytes, File cacheFile) {
        if (classBytes == null || cacheFile == null) {
            return
        }

        FileUtils.writeByteArrayToFile(cacheFile, classBytes)
    }

    void remove(File cacheFile) {
        cacheFile?.delete()
    }

    void addIncludeJar(String jarPath) {
        if (jarPath != null) {
            includeJarInfos.put(jarPath, new JarInfo(path: jarPath))
        }
    }

    void removeIncludeJar(String jarPath) {
        includeJarInfos.remove(jarPath)
    }

    boolean isIncludeJar(String jarPath) {
        if (jarPath == null) {
            return false
        }

        return includeJarInfos.containsKey(jarPath)
    }

    void commitIncludeJarConfig() {
        FileUtils.deleteQuietly(includeJarConfig)

        if (!includeJarConfig.exists()) {
            includeJarConfig.createNewFile()
        }

        List<JarInfo> jarInfoList = new ArrayList<>()
        includeJarInfos.each {String key, JarInfo value ->
            jarInfoList.add(value)
        }

        FileUtils.write(includeJarConfig, AJXUtils.optToJsonString(jarInfoList), "UTF-8")
    }

    void reset() {
        close()

        init()
    }

    void close() {
        FileUtils.deleteDirectory(cacheDir)
        includeJarInfos.clear()
    }
}
