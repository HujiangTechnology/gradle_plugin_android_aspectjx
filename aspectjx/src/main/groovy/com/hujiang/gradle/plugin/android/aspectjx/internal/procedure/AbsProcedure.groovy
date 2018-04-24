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
package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.AJXCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.model.AJXExtensionConfig
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
abstract class AbsProcedure {

    List<? extends AbsProcedure> procedures = new ArrayList<>()
    Project project
    AJXCache ajxCache
    AJXExtensionConfig ajxExtensionConfig
    VariantCache variantCache
    TransformInvocation transformInvocation

    AbsProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        this.project = project
        if (transformInvocation != null) {
            this.transformInvocation = transformInvocation
        }

        if (variantCache != null) {
            this.variantCache = variantCache
            this.ajxCache = variantCache.ajxCache
            this.ajxExtensionConfig = ajxCache.ajxExtensionConfig
        }
    }

    public <T extends AbsProcedure> AbsProcedure with(T procedure) {
        if (procedure != null) {
            procedures << procedure
        }

        return this
    }

    boolean doWorkContinuously() {
        for (AbsProcedure procedure : procedures) {
            if (!procedure.doWorkContinuously()) {
                break
            }
        }
        return true
    }
}
