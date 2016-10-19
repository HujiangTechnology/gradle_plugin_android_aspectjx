package com.hujiang.gradle.plugin.android.aspectjx;

import com.android.annotations.NonNull;

import java.io.File;
import java.io.IOException;

/**
 * Created by dim on 16/10/18.
 */

public class FileUtils {

    public static void deleteFolder(@NonNull final File folder) throws IOException {
        if (!folder.exists()) {
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

    public static void mkdirs(@NonNull File folder) {
        // attempt to create first.
        // if failure only throw if folder does not exist.
        // This makes this method able to create the same folder(s) from different thread
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
