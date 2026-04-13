package org.czbiohub.royerlab;/*-
 * #%L
 * Ultrack: Large-Scale Multi-Hypotheses Cell Tracking Using Ultrametric Contours Maps.
 * %%
 * Copyright (C) 2010 - 2024 RoyerLab.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class CondaEnvironmentFinderTest {

    @TempDir
    Path tempDir;

    /**
     * Verifies that getUltrackPath finds the executable in a conda env without
     * opening any dialog.  We write a fake ultrack binary into a temp directory,
     * save that directory as the "condaEnv" preference, then call getUltrackPath().
     */
    @Test
    void getUltrackPath_executableInCondaEnv_returnsPathWithoutDialog() throws Exception {
        // Create a fake ultrack binary under <tempDir>/bin/ultrack
        Path binDir = Files.createDirectories(tempDir.resolve("bin"));
        Path fakeBin = binDir.resolve("ultrack");
        Files.createFile(fakeBin);
        makeExecutable(fakeBin.toFile());

        // Point the user preference at our temp env so no dialog is shown.
        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        String previousCondaEnv = prefs.get("condaEnv", null);
        prefs.put("condaEnv", tempDir.toString());

        try {
            String result = CondaEnvironmentFinder.getUltrackPath();
            assertNotNull(result, "Should find the fake ultrack binary");
            assertTrue(result.endsWith("ultrack") || result.endsWith("ultrack.exe"),
                    "Returned path should end with the binary name: " + result);
            assertTrue(new File(result).canExecute(), "Returned path must be executable");
        } finally {
            // Restore the original preference to avoid polluting other tests.
            if (previousCondaEnv != null) {
                prefs.put("condaEnv", previousCondaEnv);
            } else {
                prefs.remove("condaEnv");
            }
        }
    }

    /**
     * Covers the Windows path: <tempDir>/Scripts/ultrack.exe
     */
    @Test
    void getUltrackPath_executableInScriptsDir_returnsPath() throws Exception {
        Path scriptsDir = Files.createDirectories(tempDir.resolve("Scripts"));
        Path fakeExe = scriptsDir.resolve("ultrack.exe");
        Files.createFile(fakeExe);
        makeExecutable(fakeExe.toFile());

        Preferences prefs = Preferences.userNodeForPackage(CondaEnvironmentFinder.class);
        String previousCondaEnv = prefs.get("condaEnv", null);
        prefs.put("condaEnv", tempDir.toString());

        try {
            String result = CondaEnvironmentFinder.getUltrackPath();
            assertNotNull(result);
            assertTrue(new File(result).canExecute());
        } finally {
            if (previousCondaEnv != null) {
                prefs.put("condaEnv", previousCondaEnv);
            } else {
                prefs.remove("condaEnv");
            }
        }
    }

    private static void makeExecutable(File file) throws IOException {
        // Set executable on POSIX; on Windows canExecute() checks file extension (.exe)
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file.toPath());
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (UnsupportedOperationException ignored) {
            // Windows: .exe extension is checked by canExecute()
        }
        assertTrue(file.setExecutable(true), "Failed to set executable bit");
    }
}
