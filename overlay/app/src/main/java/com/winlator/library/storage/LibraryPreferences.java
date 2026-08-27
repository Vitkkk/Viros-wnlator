package com.winlator.library.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class LibraryPreferences {
    private static final String PREFS = "viros_library";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String KEY_LOCAL_ROOT = "local_root";
    private static final String KEY_AUTO_CONTAINER_ID = "auto_container_id";

    private final SharedPreferences preferences;

    public LibraryPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Uri getTreeUri() {
        String value = preferences.getString(KEY_TREE_URI, null);
        return value != null ? Uri.parse(value) : null;
    }

    public String getLocalRoot() {
        return preferences.getString(KEY_LOCAL_ROOT, null);
    }

    public void setLibrary(Uri treeUri, String localRoot) {
        preferences.edit()
            .putString(KEY_TREE_URI, treeUri.toString())
            .putString(KEY_LOCAL_ROOT, localRoot)
            .apply();
    }

    public void clearLibrary() {
        preferences.edit()
            .remove(KEY_TREE_URI)
            .remove(KEY_LOCAL_ROOT)
            .apply();
    }

    public int getAutoContainerId() {
        return preferences.getInt(KEY_AUTO_CONTAINER_ID, 0);
    }

    public void setAutoContainerId(int id) {
        preferences.edit().putInt(KEY_AUTO_CONTAINER_ID, id).apply();
    }
}
