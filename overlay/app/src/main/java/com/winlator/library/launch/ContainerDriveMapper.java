package com.winlator.library.launch;

import com.winlator.container.Container;
import com.winlator.container.Drive;

import java.io.File;
import java.io.IOException;

final class ContainerDriveMapper {
    private static final String LIBRARY_DRIVE = "V";

    private ContainerDriveMapper() {}

    static boolean ensureVisible(Container container, String localLibraryRoot, String executablePath) {
        if (container == null || executablePath == null || executablePath.isEmpty()) return false;

        String canonicalExecutable = canonical(executablePath);
        if (canonicalExecutable == null) return false;

        for (Drive drive : container.drivesIterator()) {
            String canonicalDrive = canonical(drive.path);
            if (canonicalDrive != null && contains(canonicalDrive, canonicalExecutable)) return true;
        }

        String canonicalRoot = canonical(localLibraryRoot);
        if (canonicalRoot == null || !contains(canonicalRoot, canonicalExecutable)) return false;

        StringBuilder rebuilt = new StringBuilder();
        boolean libraryDriveWritten = false;
        for (Drive drive : container.drivesIterator()) {
            if (LIBRARY_DRIVE.equalsIgnoreCase(drive.letter)) {
                rebuilt.append(LIBRARY_DRIVE).append(':').append(canonicalRoot);
                libraryDriveWritten = true;
            }
            else {
                rebuilt.append(drive.letter).append(':').append(drive.path);
            }
        }
        if (!libraryDriveWritten) rebuilt.append(LIBRARY_DRIVE).append(':').append(canonicalRoot);

        container.setDrives(rebuilt.toString());
        container.saveData();
        return true;
    }

    private static boolean contains(String root, String child) {
        return child.equals(root) || child.startsWith(root + File.separator);
    }

    private static String canonical(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            return new File(path).getCanonicalPath();
        }
        catch (IOException e) {
            return null;
        }
    }
}
