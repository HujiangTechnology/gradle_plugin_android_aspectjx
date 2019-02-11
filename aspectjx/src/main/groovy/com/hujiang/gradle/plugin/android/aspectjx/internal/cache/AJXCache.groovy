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

import com.android.builder.model.AndroidProject
import com.hujiang.gradle.plugin.android.aspectjx.AJXExtension
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.model.AJXExtensionConfig
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-03
 */
class AJXCache {

    Project project
    String cachePath
    Map<String, VariantCache> variantCacheMap = new HashMap<>()

    String extensionConfigPath
    AJXExtensionConfig ajxExtensionConfig = new AJXExtensionConfig()

    //for aspectj
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility
    List<String> ajcArgs = new ArrayList<>()

    AJXCache(Project proj) {
        this.project = proj

        init()
    }

    private void init() {
        cachePath = project.buildDir.absolutePath + File.separator + AndroidProject.FD_INTERMEDIATES + "/ajx"
        extensionConfigPath = cachePath + File.separator + "extensionconfig.json"

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        //extension config
        File extensionConfig = new File(extensionConfigPath)
        if (extensionConfig.exists()) {
            ajxExtensionConfig = AJXUtils.optFromJsonString(FileUtils.readFileToString(extensionConfig), AJXExtensionConfig.class)
        }

        if (ajxExtensionConfig == null) {
            ajxExtensionConfig = new AJXExtensionConfig()
        }
    }

    File getCacheDir() {
        return new File(cachePath)
    }

    File getExtensionConfigFile() {
        return new File(extensionConfigPath)
    }

    void reset() {
        FileUtils.deleteDirectory(cacheDir)

        init()
    }

    void commit() {
        project.logger.debug("putExtensionConfig:${extensionConfigFile}")

        FileUtils.deleteQuietly(extensionConfigFile)

        File parent = extensionConfigFile.parentFile

        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (!extensionConfigFile.exists()) {
            extensionConfigFile.createNewFile()
        }

        String jsonString = AJXUtils.optToJsonString(ajxExtensionConfig)
        project.logger.debug("${jsonString}")
        FileUtils.write(extensionConfigFile, jsonString, "UTF-8")
    }

    void put(String variantName, VariantCache cache) {
        if (variantName != null && cache != null) {
            variantCacheMap.put(variantName, cache)
        }
    }

    boolean contains(String variantName) {
        if (variantName == null) {
            return false
        }

        return variantCacheMap.containsKey(variantName)
    }

    void putExtensionConfig(AJXExtension extension) {
        if (extension == null) {
            return
        }

        ajxExtensionConfig.enabled = extension.enabled
        ajxExtensionConfig.ajcArgs = extension.ajcArgs
        ajxExtensionConfig.includes = extension.includes
        ajxExtensionConfig.excludes = extension.excludes
    }

    boolean isExtensionChanged(AJXExtension extension) {
        if (extension == null) {
            return true
        }

        boolean isSourceIncludesExists = ajxExtensionConfig.includes != null && !ajxExtensionConfig.includes.isEmpty()
        boolean isTargetIncludeExists = extension.includes != null && !extension.includes.isEmpty()
        boolean isSourceExcludeExists = ajxExtensionConfig.excludes != null && !ajxExtensionConfig.excludes.isEmpty()
        boolean isTargetExcludeExists = extension.excludes != null && !extension.excludes.isEmpty()

        if ((!isSourceIncludesExists && isTargetIncludeExists)
            || (isSourceIncludesExists && !isTargetIncludeExists)
            || (!isSourceExcludeExists && isTargetExcludeExists)
            || (isSourceExcludeExists && !isTargetExcludeExists)) {
            return true
        }

        if ((!isSourceIncludesExists && !isTargetIncludeExists)
            && (!isSourceExcludeExists && !isTargetExcludeExists)) {
            return false
        }

        if (ajxExtensionConfig.includes.size() != extension.includes.size()
            || ajxExtensionConfig.excludes.size() != extension.excludes.size()) {
            return true
        }

        boolean isChanged = false
        ajxExtensionConfig.includes.each {String source ->
            boolean targetMatched = false
            for (String target : extension.includes) {
                if (source == target) {
                    targetMatched = true
                    break
                }
            }

            if (!targetMatched) {
                isChanged = true
            }
        }

        ajxExtensionConfig.excludes.each {String source ->
            boolean targetMatched = false
            for (String target : extension.excludes) {
                if (source == target) {
                    targetMatched = true
                    break
                }
            }

            if (!targetMatched) {
                isChanged = true
            }
        }

        return isChanged
    }
}
