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
package com.hujiang.gradle.plugin.android.aspectjx


/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2016-05-05
 */
class AJXExtension {

    List<String> includes = new ArrayList<>()
    List<String> excludes = new ArrayList<>()

    List<String> ajcArgs=new ArrayList<>()

    boolean enabled = true


    AJXExtension include(String...filters) {
        if (filters != null) {
            this.includes.addAll(filters)
        }

        return this
    }

    AJXExtension exclude(String...filters) {
        if (filters != null) {
            this.excludes.addAll(filters)
        }

        return this
    }

    AJXExtension ajcArgs(String...ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }

        return this
    }
}
