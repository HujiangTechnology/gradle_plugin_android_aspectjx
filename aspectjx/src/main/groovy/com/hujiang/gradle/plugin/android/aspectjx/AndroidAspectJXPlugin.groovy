// This plugin is based on https://github.com/JakeWharton/hugo
package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * aspectj plugin,
 * @author simon
 * @version 1.0.0
 * @since 2016-04-20
 */
class AndroidAspectJXPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.repositories {
            mavenLocal()
        }

        project.dependencies {
            compile 'org.aspectj:aspectjrt:1.8.+'
        }

        project.extensions.create("aspectjx", AspectjExtension)

        if (project.plugins.hasPlugin(AppPlugin)) {
            //build time trace
            project.gradle.addListener(new TimeTrace())

            if (project.aspectjx.enable) {
                //register AspectTransform
                AppExtension android = project.extensions.getByType(AppExtension)
                android.registerTransform(new AspectTransform(project))
            }
        }
    }
}
