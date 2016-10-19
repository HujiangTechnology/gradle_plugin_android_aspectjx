/*
 * FileUtils		2016-10-19
 * Copyright (c) 2016 hujiang Co.Ltd. All right reserved(http://www.hujiang.com).
 * 
 */
package com.hujiang.gradle.plugin.android.aspectjx;

import com.android.annotations.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * File utils
 *
 * @author simon
 * @version 1.0.0
 * @since 2016-10-19
 */
public class FileUtils {

    public static void deleteFolder(File folder) throws IOException {
        if (folder == null || !folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) { // i.e. is a directory.
            for (final File file : files) {
                deleteFolder(file);
            }
        }
        if (!folder.delete()) {
            throw new IOException(String.format("Could not delete folder %s", folder));
        }
    }

    public static void mkdirs(File folder) {
        if (folder == null) {
            return;
        }

        if (!folder.mkdirs() && !folder.exists()) {
            throw new RuntimeException("Cannot create directory " + folder);
        }
    }

    public static void deleteIfExists(@NonNull File file) throws IOException {
        boolean result = file.delete();
        if (!result && file.exists()) {
            throw new IOException("Failed to delete " + file.getAbsolutePath());
        }
    }
}
