package com.hujiang.gradle.plugin.android.aspectjx

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2016-05-05
 */
class AspectjExtension {

    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()
    List<String> ajcArgs=new ArrayList<>();

    public AspectjExtension includeJarFilter(String...filters) {
        if (filters != null) {
            includeJarFilter.addAll(filters)
        }

        return this
    }

    public AspectjExtension excludeJarFilter(String...filters) {
        if (filters != null) {
            excludeJarFilter.addAll(filters)
        }

        return this
    }
    public AspectjExtension ajcArgs(String...ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }
        return this
    }
}
