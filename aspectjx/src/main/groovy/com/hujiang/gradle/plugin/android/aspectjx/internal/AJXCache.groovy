package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.android.builder.model.AndroidProject
import com.google.gson.reflect.TypeToken
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.util.TextUtil

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-03
 */
class AJXCache {

    Project project
    String variantName
    String cachePath
    String aspectPath
    String includeFilePath
    String excludeFilePath
    String includeJarConfigPath

    List<IncludeJarInfo> includeJarInfos = new ArrayList<>()

    AJXCache(Project proj, String variantName) {
        this.project = proj
        this.variantName = variantName

        init()
    }

    private void init() {
        cachePath = project.buildDir.absolutePath + File.separator + AndroidProject.FD_INTERMEDIATES + "/ajx/" + variantName
        aspectPath = cachePath + File.separator + "aspecs"
        includeFilePath = cachePath + File.separator + "includefiles"
        excludeFilePath = cachePath + File.separator + "excludefiles"
        includeJarConfigPath = cachePath + File.separator + "includejars.json"

        File aspectDir = new File(aspectPath)
        if (!aspectDir.exists()) {
            aspectDir.mkdirs()
        }

        File includeDir = new File(includeFilePath)
        if (!includeDir.exists()) {
            includeDir.mkdirs()
        }

        File excludeDir = new File(excludeFilePath)
        if (!excludeDir.exists()) {
            excludeDir.mkdirs()
        }

        File includeJarConfig = new File(includeJarConfigPath)
        if (includeJarConfig.exists()) {
            includeJarInfos = AJXUtils.optFromJsonString(FileUtils.readFileToString(includeJarConfig), new TypeToken<List<IncludeJarInfo>>(){}.getType())
        }

        if (includeJarInfos == null) {
            includeJarInfos = new ArrayList<>()
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
        includeJarInfos.add(new IncludeJarInfo(path: jarPath))
    }

    void removeIncludeJar(String jarPath) {
        for (IncludeJarInfo info : includeJarInfos) {
            if (info.path == jarPath) {
                includeJarInfos.remove(info)
                break
            }
        }
    }

    void commit() {
        FileUtils.deleteQuietly(includeJarConfig)

        includeJarConfig.createNewFile()
        FileUtils.write(includeJarConfig, AJXUtils.optToJsonString(includeJarInfos), "UTF-8")
    }

    void reset() {
        FileUtils.deleteDirectory(cacheDir)
//        FileUtils.deleteDirectory(includeFileDir)
//        FileUtils.deleteDirectory(excludeFileDir)
//        FileUtils.deleteQuietly(includeJarConfig)
        includeJarInfos.clear()

        init()
    }

    boolean isIncludeJar(String jarPath) {
        if (jarPath == null) {
            return false
        }

        for (IncludeJarInfo info : includeJarInfos) {
            if (info.path == jarPath) {
                return true
            }
        }

        return false
    }
}
