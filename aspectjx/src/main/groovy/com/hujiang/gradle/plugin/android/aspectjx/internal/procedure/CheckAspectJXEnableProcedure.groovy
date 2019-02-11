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
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class CheckAspectJXEnableProcedure extends AbsProcedure {

    CheckAspectJXEnableProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
    }

    @Override
    boolean doWorkContinuously() {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~~~~ check aspectjx enable")

        //check if exclude all files or not
        boolean isExcludeAll = false
        for (String filter : ajxExtensionConfig.excludes) {
            if (filter == "*" || filter == "**") {
                isExcludeAll = true
                break
            }
        }

        //check if include all files or not
        boolean isIncludeAll = false
        for (String filter : ajxExtensionConfig.includes) {
            if (filter == "*" || filter == "**") {
                isIncludeAll = true
                break
            }
        }

        if (isIncludeAll) {
            ajxExtensionConfig.includes.clear()
        }

        //aspectjx disabled
        if (!ajxExtensionConfig.enabled || isExcludeAll) {
            AJXUtils.doWorkWithNoAspectj(transformInvocation)
            return false
        }

        return true
    }
}
