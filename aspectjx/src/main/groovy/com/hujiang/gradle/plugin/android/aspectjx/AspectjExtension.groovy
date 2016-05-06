package com.hujiang.gradle.plugin.android.aspectjx

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2016-05-05
 */
class AspectjExtension {

    List<String> jarFilter = new ArrayList<String>()

    public AspectjExtension jarFilter(String...filters) {
        if (filters != null) {
            jarFilter.addAll(filters)
        }

        return this;
    }
}
