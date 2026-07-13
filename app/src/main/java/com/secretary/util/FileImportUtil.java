package com.secretary.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Utility for importing files into the app's private storage.
 * Files are copied to Context.getExternalFilesDir("private_storage")
 * so they are invisible to other apps and the system gallery.
 */
public class FileImportUtil {

    /**
     * Copy a file from content URI to app-private storage.
     * Returns a Single emitting the FileInfo on success.
     */
    public static Single<FileInfo> importFile(Context context, Uri sourceUri) {
        return Single.<FileInfo>create(emitter -> {
            try {
                ContentResolver resolver = context.getContentResolver();
                String fileName = getFileName(context, sourceUri);
                String mimeType = resolver.getType(sourceUri);
                if (fileName == null) {
                    fileName = "file_" + System.currentTimeMillis();
                }

                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = fileName.substring(dotIndex);
                } else if (mimeType != null) {
                    String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (ext != null) extension = "." + ext;
                }

                String subDir;
                String fileType = getFileType(mimeType, extension);
                switch (fileType) {
                    case "image": subDir = "images"; break;
                    case "video": subDir = "videos"; break;
                    case "audio": subDir = "audios"; break;
                    default: subDir = "others"; break;
                }

                File privateDir = new File(context.getExternalFilesDir("private_storage"), subDir);
                if (!privateDir.exists()) privateDir.mkdirs();

                File targetFile = new File(privateDir, fileName);
                int counter = 1;
                while (targetFile.exists()) {
                    String base = fileName;
                    String ext = "";
                    int extDot = fileName.lastIndexOf('.');
                    if (extDot > 0) {
                        base = fileName.substring(0, extDot);
                        ext = fileName.substring(extDot);
                    }
                    targetFile = new File(privateDir, base + "_(" + counter + ")" + ext);
                    counter++;
                }

                InputStream inputStream = resolver.openInputStream(sourceUri);
                if (inputStream == null) {
                    emitter.onError(new Exception("Cannot open input stream"));
                    return;
                }

                BufferedInputStream bis = new BufferedInputStream(inputStream);
                FileOutputStream fos = new FileOutputStream(targetFile);

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalRead = 0;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                bis.close();
                fos.close();

                FileInfo info = new FileInfo();
                info.path = targetFile.getAbsolutePath();
                info.name = targetFile.getName();
                info.size = totalRead;
                info.type = fileType;
                info.mimeType = mimeType != null ? mimeType : getMimeType(extension);

                emitter.onSuccess(info);

            } catch (Exception e) {
                emitter.onError(e);
            }
        }).subscribeOn(Schedulers.io());
    }

    private static String getFileName(Context context, Uri uri) {
        String name = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) name = cursor.getString(nameIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        if (name == null) name = uri.getLastPathSegment();
        return name;
    }

    private static boolean isImage(String mimeType, String extension) {
        if (mimeType != null && mimeType.startsWith("image/")) return true;
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".gif") || ext.equals(".bmp") || ext.equals(".webp");
    }

    private static boolean isVideo(String mimeType, String extension) {
        if (mimeType != null && mimeType.startsWith("video/")) return true;
        String ext = extension.toLowerCase();
        return ext.equals(".mp4") || ext.equals(".avi") || ext.equals(".mkv")
                || ext.equals(".mov") || ext.equals(".wmv") || ext.equals(".flv")
                || ext.equals(".3gp");
    }

    private static boolean isAudio(String mimeType, String extension) {
        if (mimeType != null && mimeType.startsWith("audio/")) return true;
        String ext = extension.toLowerCase();
        return ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".aac")
                || ext.equals(".flac") || ext.equals(".ogg") || ext.equals(".m4a");
    }

    public static String getFileType(String mimeType, String extension) {
        if (isImage(mimeType, extension)) return "image";
        if (isVideo(mimeType, extension)) return "video";
        if (isAudio(mimeType, extension)) return "audio";
        return "other";
    }

    private static String getMimeType(String extension) {
        if (extension == null) return "*/*";
        String mime = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.replace(".", ""));
        return mime != null ? mime : "*/*";
    }

    public static class FileInfo {
        public String path;
        public String name;
        public long size;
        public String type;
        public String mimeType;
    }
}
