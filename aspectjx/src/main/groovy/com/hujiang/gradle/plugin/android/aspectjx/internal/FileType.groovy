package com.hujiang.gradle.plugin.android.aspectjx.internal

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-02-01
 */
enum FileType {
    DEFAULT,
    /**
     * class file like 'xxx.class'
     */
    CLASS,
    /**
     * java source file like 'xxx.java'
     */
    JAVA,
    /**
     * groovy source file like 'xxx.groovy'
     */
    GROOVY,
    /**
     * kotlin source file like 'kotlin'
     */
    KOTLIN,
    /**
     * jar file like 'xxx.jar'
     */
    JAR
}
