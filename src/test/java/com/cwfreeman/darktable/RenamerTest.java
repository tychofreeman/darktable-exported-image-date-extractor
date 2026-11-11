package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenamerTest {
    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
    private final PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8);

    private static RenamePlan plan(RenamePlan.Rename... renames) {
        return new RenamePlan(List.of(renames), List.of());
    }

    @Test
    void aRenameThatFailsIsReportedAndTheRunFails() {
        File missing = dir.resolve("missing.png").toFile();
        RenamePlan.Rename rename = new RenamePlan.Rename(missing, dir.resolve("2024-12-30_23-59-58.png").toFile());
        assertEquals(1, new FileRenamer(outStream, errStream).run(plan(rename)));
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("could not rename " + missing + ": "));
    }

    @Test
    void theRenamesAfterOneThatFailsAreStillMade() throws Exception {
        Path photo = Files.createFile(dir.resolve("DSC_2584.png"));
        Path renamed = dir.resolve("2024-12-30_23-59-58.png");
        assertEquals(1, new FileRenamer(outStream, errStream).run(plan(
                new RenamePlan.Rename(dir.resolve("missing.png").toFile(), dir.resolve("other.png").toFile()),
                new RenamePlan.Rename(photo.toFile(), renamed.toFile()))));
        assertTrue(Files.exists(renamed));
    }

    @Test
    void reportsTheSkippedFiles() {
        File file = dir.resolve("no-exif.png").toFile();
        RenamePlan plan = new RenamePlan(List.of(), List.of(new RenamePlan.Skipped(file, "no EXIF DateTimeOriginal")));
        assertEquals(0, new Renamer(outStream, errStream).run(plan));
        assertEquals("skipped " + file + ": no EXIF DateTimeOriginal" + System.lineSeparator(), err.toString(StandardCharsets.UTF_8));
    }
}
