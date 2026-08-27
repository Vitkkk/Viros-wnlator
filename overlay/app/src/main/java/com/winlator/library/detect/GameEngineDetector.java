package com.winlator.library.detect;

import androidx.documentfile.provider.DocumentFile;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class GameEngineDetector {
    private GameEngineDetector() {}

    public static String detect(DocumentFile gameFolder) {
        if (gameFolder == null || !gameFolder.isDirectory()) return "generic";

        Set<String> names = new HashSet<>();
        DocumentFile[] children = gameFolder.listFiles();
        for (DocumentFile child : children) {
            String name = child.getName();
            if (name != null) names.add(name.toLowerCase(Locale.ROOT));
        }

        boolean unityData = false;
        boolean hasRpa = false;
        boolean hasPck = false;
        for (String name : names) {
            if (name.endsWith("_data")) unityData = true;
            if (name.endsWith(".rpa")) hasRpa = true;
            if (name.endsWith(".pck")) hasPck = true;
        }

        if (names.contains("unityplayer.dll") && unityData) return "unity";
        if (names.contains("engine") && (names.contains("binaries") || names.contains("content"))) return "unreal";
        if (hasPck) return "godot";
        if (names.contains("data.win")) return "gamemaker";
        if (names.contains("renpy") || hasRpa) return "renpy";
        if (names.contains("www") && (names.contains("game.exe") || names.contains("nw.exe"))) return "rpgmaker";
        if (names.contains("resources") || names.contains("app.asar")) {
            DocumentFile resources = gameFolder.findFile("resources");
            if (resources != null && resources.isDirectory() && resources.findFile("app.asar") != null) return "electron";
            if (names.contains("app.asar")) return "electron";
        }
        return "generic";
    }
}
