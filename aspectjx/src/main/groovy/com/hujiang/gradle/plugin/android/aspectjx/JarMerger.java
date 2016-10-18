package com.hujiang.gradle.plugin.android.aspectjx;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.utils.ILogger;
import com.google.common.io.Closer;

import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Created by dim on 16/10/18.
 */

public class JarMerger {

    private final byte[] buffer = new byte[8192];

    @NonNull
    private final ILogger logger = new LoggerWrapper(Logging.getLogger(com.android.build.gradle.internal.transforms.JarMerger.class));

    @NonNull
    private final File jarFile;
    private Closer closer;
    private JarOutputStream jarOutputStream;
    private IZipEntryFilter filter;

    public JarMerger(@NonNull File jarFile) throws IOException {
        this.jarFile = jarFile;
    }

    private void init() throws IOException {
        if (closer == null) {
            FileUtils.mkdirs(jarFile.getParentFile());

            closer = Closer.create();

            FileOutputStream fos = closer.register(new FileOutputStream(jarFile));
            jarOutputStream = closer.register(new JarOutputStream(fos));
        }
    }

    /**
     * Sets a list of regex to exclude from the jar.
     */
    public void setFilter(IZipEntryFilter filter) {
        this.filter = filter;
    }

    public void addFolder(@NonNull File folder) throws IOException {
        init();
        try {
            addFolder(folder, "");
        } catch (IOException e) {
            throw e;
        }
    }

    private void addFolder(File folder, @NonNull String path) throws IOException {
        logger.verbose("addFolder(%1$s, %2$s)", folder, path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String entryPath = path + file.getName();
                    if (filter == null || filter.checkEntry(entryPath)) {
                        logger.verbose("addFolder (%1$s, %2$s): entry %3$s", folder, path, entryPath);
                        // new entry
                        jarOutputStream.putNextEntry(new JarEntry(entryPath));

                        // put the file content
                        Closer localCloser = Closer.create();
                        try {
                            FileInputStream fis = localCloser.register(new FileInputStream(file));
                            int count;
                            while ((count = fis.read(buffer)) != -1) {
                                jarOutputStream.write(buffer, 0, count);
                            }
                        } finally {
                            localCloser.close();
                        }

                        // close the entry
                        jarOutputStream.closeEntry();
                    }
                } else if (file.isDirectory()) {
                    addFolder(file, path + file.getName() + "/");
                }
            }
        }
    }

    public void close() throws IOException {
        if (closer != null) {
            closer.close();
        }
    }

    public interface IZipEntryFilter {
        boolean checkEntry(String archivePath);
    }
}
