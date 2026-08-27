package com.winlator.library.scan;

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.winlator.library.data.GameEntity;
import com.winlator.library.detect.GameEngineDetector;
import com.winlator.library.storage.SafPathResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GameScanner {
    private static final int MAX_DEPTH = 5;
    private static final int MAX_EXECUTABLES_PER_GROUP = 256;

    private static final Set<String> EXCLUDED_EXACT = new HashSet<>(Arrays.asList(
        "uninstall.exe", "setup.exe", "installer.exe", "crashhandler.exe",
        "unitycrashhandler.exe", "unitycrashhandler64.exe", "dxsetup.exe",
        "updater.exe", "update.exe"
    ));

    public static class Candidate {
        public final String displayName;
        public final String gameFolderUri;
        public final String executableUri;
        public final String relativePath;
        public final String resolvedLocalPath;
        public final String detectedEngine;
        public final long size;
        public final long lastModified;
        public final int score;

        private Candidate(
            String displayName,
            String gameFolderUri,
            String executableUri,
            String relativePath,
            String resolvedLocalPath,
            String detectedEngine,
            long size,
            long lastModified,
            int score
        ) {
            this.displayName = displayName;
            this.gameFolderUri = gameFolderUri;
            this.executableUri = executableUri;
            this.relativePath = relativePath;
            this.resolvedLocalPath = resolvedLocalPath;
            this.detectedEngine = detectedEngine;
            this.size = size;
            this.lastModified = lastModified;
            this.score = score;
        }

        public GameEntity toEntity() {
            GameEntity game = new GameEntity();
            game.name = displayName;
            game.folderUri = gameFolderUri;
            game.executableUri = executableUri;
            game.executableRelativePath = relativePath;
            game.resolvedLocalPath = resolvedLocalPath;
            game.detectedEngine = detectedEngine;
            game.sourceSize = size;
            game.sourceLastModified = lastModified;
            return game;
        }
    }

    public static class AmbiguousGroup {
        public final String folderName;
        public final List<Candidate> candidates;

        private AmbiguousGroup(String folderName, List<Candidate> candidates) {
            this.folderName = folderName;
            this.candidates = candidates;
        }
    }

    public static class Result {
        public final List<Candidate> automatic = new ArrayList<>();
        public final List<AmbiguousGroup> ambiguous = new ArrayList<>();
        public int visitedExecutables;
    }

    private static class RawCandidate {
        DocumentFile file;
        String relativePath;
        int depth;
    }

    public Result scan(Context context, Uri treeUri, String localRoot) {
        Result result = new Result();
        DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
        if (tree == null || !tree.isDirectory()) return result;

        List<RawCandidate> rootExecutables = new ArrayList<>();
        for (DocumentFile child : tree.listFiles()) {
            if (child.isFile() && isExecutable(child)) {
                RawCandidate raw = new RawCandidate();
                raw.file = child;
                raw.relativePath = safeName(child);
                raw.depth = 0;
                rootExecutables.add(raw);
            }
        }
        evaluateGroup(tree, rootExecutables, localRoot, result);

        for (DocumentFile child : tree.listFiles()) {
            if (!child.isDirectory()) continue;
            List<RawCandidate> raw = new ArrayList<>();
            collectExecutables(child, safeName(child), 0, raw);
            evaluateGroup(child, raw, localRoot, result);
        }
        return result;
    }

    private void collectExecutables(DocumentFile directory, String relativeBase, int depth, List<RawCandidate> out) {
        if (depth > MAX_DEPTH || out.size() >= MAX_EXECUTABLES_PER_GROUP) return;
        for (DocumentFile child : directory.listFiles()) {
            if (out.size() >= MAX_EXECUTABLES_PER_GROUP) return;
            String name = safeName(child);
            String relative = relativeBase.isEmpty() ? name : relativeBase + "/" + name;
            if (child.isFile() && isExecutable(child)) {
                RawCandidate raw = new RawCandidate();
                raw.file = child;
                raw.relativePath = relative;
                raw.depth = depth;
                out.add(raw);
            }
            else if (child.isDirectory()) {
                collectExecutables(child, relative, depth + 1, out);
            }
        }
    }

    private void evaluateGroup(DocumentFile gameFolder, List<RawCandidate> raw, String localRoot, Result result) {
        if (raw.isEmpty()) return;
        String folderName = safeName(gameFolder);
        String engine = GameEngineDetector.detect(gameFolder);
        List<Candidate> candidates = new ArrayList<>();

        for (RawCandidate item : raw) {
            result.visitedExecutables++;
            String filename = safeName(item.file);
            if (isExcluded(filename)) continue;
            int score = score(filename, folderName, item.file.length(), item.depth, engine);
            String localPath = SafPathResolver.resolveChildPath(localRoot, item.relativePath);
            if (localPath == null) continue;
            candidates.add(new Candidate(
                deriveDisplayName(folderName, filename),
                gameFolder.getUri().toString(),
                item.file.getUri().toString(),
                item.relativePath,
                localPath,
                engine,
                item.file.length(),
                item.file.lastModified(),
                score
            ));
        }

        if (candidates.isEmpty()) return;
        Collections.sort(candidates, Comparator.comparingInt((Candidate value) -> value.score).reversed());

        if (candidates.size() == 1) {
            result.automatic.add(candidates.get(0));
            return;
        }

        int gap = candidates.get(0).score - candidates.get(1).score;
        if (gap >= 15 && candidates.get(0).score >= 20) {
            result.automatic.add(candidates.get(0));
        }
        else {
            int limit = Math.min(8, candidates.size());
            result.ambiguous.add(new AmbiguousGroup(folderName, new ArrayList<>(candidates.subList(0, limit))));
        }
    }

    private static boolean isExecutable(DocumentFile file) {
        String name = file.getName();
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".exe");
    }

    private static boolean isExcluded(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        if (EXCLUDED_EXACT.contains(name)) return true;
        if (name.startsWith("unins") && name.endsWith(".exe")) return true;
        if (name.startsWith("uninstall") && name.endsWith(".exe")) return true;
        if (name.startsWith("vc_redist") && name.endsWith(".exe")) return true;
        if (name.contains("crashhandler") && name.endsWith(".exe")) return true;
        return false;
    }

    private static int score(String filename, String folderName, long size, int depth, String engine) {
        int score = 0;
        String exeBase = normalize(stripExtension(filename));
        String folderBase = normalize(folderName);

        if (!exeBase.isEmpty() && exeBase.equals(folderBase)) score += 35;
        else if (!exeBase.isEmpty() && !folderBase.isEmpty() && (exeBase.contains(folderBase) || folderBase.contains(exeBase))) score += 20;

        if (depth == 0) score += 18;
        else if (depth == 1) score += 8;
        else score -= Math.min(12, depth * 3);

        if (size >= 1024L * 1024L) score += 8;
        if (size >= 10L * 1024L * 1024L) score += 4;

        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.equals("game.exe") || lower.equals("start.exe") || lower.equals("launcher.exe")) score += 4;
        if (lower.contains("server") || lower.contains("editor") || lower.contains("benchmark")) score -= 8;
        if (!"generic".equals(engine) && depth == 0) score += 6;
        return score;
    }

    private static String deriveDisplayName(String folderName, String filename) {
        if (folderName != null && !folderName.isEmpty() && !folderName.equals("primary")) return folderName;
        return stripExtension(filename);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String stripExtension(String value) {
        int dot = value.lastIndexOf('.');
        return dot > 0 ? value.substring(0, dot) : value;
    }

    private static String safeName(DocumentFile file) {
        String name = file != null ? file.getName() : null;
        return name != null ? name : "";
    }
}
