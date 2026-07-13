package com.secretary.util;

import android.content.Context;
import android.os.Environment;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility for exporting files from app-private storage to the system Downloads directory.
 * Uses direct file I/O (works with targetSdkVersion 28 which opts out of scoped storage).
 */
public class FileExportUtil {

    /**
     * Export a single file from app-private storage to system Downloads.
     *
     * @param context    Application context
     * @param sourcePath Absolute path to the file in private storage
     * @param fileName   Display name for the exported file
     * @return Single emitting the exported file path as a String
     */
    public static Single<String> exportFile(Context context, String sourcePath, String fileName) {
        return Single.<String>create(emitter -> {
            File source = new File(sourcePath);
            if (!source.exists()) {
                emitter.onError(new Exception("源文件不存在"));
                return;
            }

            // Write directly to public Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File targetFile = new File(downloadsDir, fileName);
            targetFile = resolveConflict(targetFile);

            try (InputStream in = new FileInputStream(source);
                 OutputStream out = new FileOutputStream(targetFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            emitter.onSuccess(targetFile.getAbsolutePath());
        }).subscribeOn(Schedulers.io());
    }

    /**
     * If a file with the same name exists, append a counter before the extension.
     */
    private static File resolveConflict(File file) {
        if (!file.exists()) return file;
        String name = file.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        int counter = 1;
        File parent = file.getParentFile();
        File candidate;
        do {
            candidate = new File(parent, base + "_(" + counter + ")" + ext);
            counter++;
        } while (candidate.exists());
        return candidate;
    }
}
