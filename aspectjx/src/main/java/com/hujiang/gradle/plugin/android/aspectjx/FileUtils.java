/*
 * Copyright 2016 firefly1126, Inc.

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
