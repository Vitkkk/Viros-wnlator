package com.winlator.library.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.IOException;

public final class SafPathResolver {
    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    private SafPathResolver() {}

    public static String resolveLocalTreePath(Context context, Uri treeUri) {
        if (treeUri == null || !DocumentsContract.isTreeUri(treeUri)) return null;
        if (!EXTERNAL_STORAGE_AUTHORITY.equals(treeUri.getAuthority())) return null;

        String documentId;
        try {
            documentId = DocumentsContract.getTreeDocumentId(treeUri);
        }
        catch (Exception e) {
            return null;
        }

        String[] parts = documentId.split(":", 2);
        String volumeId = parts.length > 0 ? parts[0] : "";
        String relativePath = parts.length > 1 ? parts[1] : "";

        File volumeRoot;
        if ("primary".equalsIgnoreCase(volumeId)) {
            volumeRoot = Environment.getExternalStorageDirectory();
        }
        else if (!volumeId.isEmpty()) {
            volumeRoot = new File("/storage", volumeId);
        }
        else return null;

        try {
            File resolved = relativePath.isEmpty() ? volumeRoot : new File(volumeRoot, relativePath);
            return resolved.getCanonicalPath();
        }
        catch (IOException e) {
            return null;
        }
    }

    public static String resolveChildPath(String localRoot, String relativePath) {
        if (localRoot == null || relativePath == null) return null;
        try {
            File root = new File(localRoot).getCanonicalFile();
            File child = new File(root, relativePath).getCanonicalFile();
            String rootPath = root.getPath();
            String childPath = child.getPath();
            if (!childPath.equals(rootPath) && !childPath.startsWith(rootPath + File.separator)) return null;
            return childPath;
        }
        catch (IOException e) {
            return null;
        }
    }
}
