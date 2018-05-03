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
package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.android.annotations.NonNull
import com.google.common.io.Closer
import org.apache.commons.io.FileUtils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * class description here
 *
 * @author simon
 * @version 1.0.0
 * @since 2016-10-19
 */
class JarMerger {
    private final byte[] buffer = new byte[8192];

    @NonNull
    private final File jarFile
    private Closer closer
    private JarOutputStream jarOutputStream

    private IZipEntryFilter filter

    JarMerger(@NonNull File jarFile) throws IOException {
        this.jarFile = jarFile
    }

    private void init() throws IOException {
        if (closer == null) {
            FileUtils.forceMkdir(jarFile.getParentFile())

            closer = Closer.create()

            FileOutputStream fos = closer.register(new FileOutputStream(jarFile))
            jarOutputStream = closer.register(new JarOutputStream(fos))
        }
    }

    /**
     * Sets a list of regex to exclude from the jar.
     */
    void setFilter(@NonNull IZipEntryFilter filter) {
        this.filter = filter
    }

    void addFolder(@NonNull File folder) throws IOException {
        init()
        try {
            addFolderWithPath(folder, "")
        } catch (IZipEntryFilter.ZipAbortException e) {
            throw new IOException(e)
        }
    }

    private void addFolderWithPath(@NonNull File folder, @NonNull String path)
            throws IOException, IZipEntryFilter.ZipAbortException {
        File[] files = folder.listFiles()
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String entryPath = path + file.getName()
                    if (filter == null || filter.checkEntry(entryPath)) {
                        // new entry
                        jarOutputStream.putNextEntry(new JarEntry(entryPath))

                        // put the file content
                        Closer localCloser = Closer.create()
                        try {
                            FileInputStream fis = localCloser.register(new FileInputStream(file))
                            int count
                            while ((count = fis.read(buffer)) != -1) {
                                jarOutputStream.write(buffer, 0, count)
                            }
                        } finally {
                            localCloser.close()
                        }

                        // close the entry
                        jarOutputStream.closeEntry()
                    }
                } else if (file.isDirectory()) {
                    addFolderWithPath(file, path + file.getName() + "/")
                }
            }
        }
    }

    void addJar(@NonNull File file) throws IOException {
        addJar(file, false)
    }

    void addJar(@NonNull File file, boolean removeEntryTimestamp) throws IOException {
        init()

        Closer localCloser = Closer.create()
        try {
            FileInputStream fis = localCloser.register(new FileInputStream(file))
            ZipInputStream zis = localCloser.register(new ZipInputStream(fis))

            // loop on the entries of the jar file package and put them in the final jar
            ZipEntry entry
            while ((entry = zis.getNextEntry()) != null) {
                // do not take directories or anything inside a potential META-INF folder.
                if (entry.isDirectory()) {
                    continue
                }

                String name = entry.getName()
                if (filter != null && !filter.checkEntry(name)) {
                    continue
                }

                JarEntry newEntry

                // Preserve the STORED method of the input entry.
                if (entry.getMethod() == JarEntry.STORED) {
                    newEntry = new JarEntry(entry)
                } else {
                    // Create a new entry so that the compressed len is recomputed.
                    newEntry = new JarEntry(name)
                }
                if (removeEntryTimestamp) {
                    newEntry.setTime(0)
                }

                // add the entry to the jar archive
                jarOutputStream.putNextEntry(newEntry)

                // read the content of the entry from the input stream, and write it into the archive.
                int count
                while ((count = zis.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, count)
                }

                // close the entries for this file
                jarOutputStream.closeEntry()
                zis.closeEntry()
            }
        } catch (IZipEntryFilter.ZipAbortException e) {
            throw new IOException(e)
        } finally {
            localCloser.close()
        }
    }

    public void addEntry(@NonNull String path, @NonNull byte[] bytes) throws IOException {
        init()

        jarOutputStream.putNextEntry(new JarEntry(path))
        jarOutputStream.write(bytes)
        jarOutputStream.closeEntry()
    }

    public void close() throws IOException {
        if (closer != null) {
            closer.close()
        }
    }

    /**
     * Classes which implement this interface provides a method to check whether a file should
     * be added to a Jar file.
     */
    public static interface IZipEntryFilter {
        /**
         * An exception thrown during packaging of a zip file into APK file.
         * This is typically thrown by implementations of
         * {@link IZipEntryFilter#checkEntry(String)}.
         */
        class ZipAbortException extends Exception {
            private static final long serialVersionUID = 1L

            public ZipAbortException() {
                super()
            }

            public ZipAbortException(String format, Object... args) {
                super(String.format(format, args))
            }

            public ZipAbortException(Throwable cause, String format, Object... args) {
                super(String.format(format, args), cause)
            }

            public ZipAbortException(Throwable cause) {
                super(cause)
            }
        }


        /**
         * Checks a file for inclusion in a Jar archive.
         * @param archivePath the archive file path of the entry
         * @return <code>true</code> if the file should be included.
         * @throws IZipEntryFilter.ZipAbortException if writing the file should be aborted.
         */
        boolean checkEntry(String archivePath) throws IZipEntryFilter.ZipAbortException
    }
}