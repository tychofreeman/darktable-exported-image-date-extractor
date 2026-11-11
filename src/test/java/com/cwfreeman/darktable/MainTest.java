package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @TempDir
    Path dir;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private Path copy(String fixture, String name) throws Exception {
        return Files.copy(DateTakenTest.fixture(fixture).toPath(), dir.resolve(name));
    }

    @Test
    void printsTheNewNamesWithoutRenaming() throws Exception {
        Path photo = copy("darktable-5.4-exif.png", "DSC_2584.png");
        assertEquals(0, run(photo.toString()));
        assertEquals(photo + " -> 2024-12-30_23-59-58.png" + System.lineSeparator(), out.toString(StandardCharsets.UTF_8));
        assertTrue(Files.exists(photo));
    }

    @Test
    void renamesWhenAsked() throws Exception {
        Path photo = copy("darktable-5.4-exif.png", "DSC_2584.png");
        assertEquals(0, run("--rename", photo.toString()));
        assertFalse(Files.exists(photo));
        assertTrue(Files.exists(dir.resolve("2024-12-30_23-59-58.png")));
    }

    @Test
    void takesTheImagesInADirectory() throws Exception {
        copy("darktable-5.4-exif.png", "DSC_1.png");
        copy("raw-profile-exif.png", "DSC_2.jpg");
        Files.writeString(dir.resolve("DSC_1.png.xmp"), "<x:xmpmeta/>");
        assertEquals(0, run(dir.toString()));
        assertEquals(dir.resolve("DSC_1.png") + " -> 2024-12-30_23-59-58.png" + System.lineSeparator()
                        + dir.resolve("DSC_2.jpg") + " -> 2024-12-30_23-59-58.jpg" + System.lineSeparator(),
                out.toString(StandardCharsets.UTF_8));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsSkippedFiles() throws Exception {
        Path photo = copy("no-exif.png", "DSC_2584.png");
        assertEquals(0, run(photo.toString()));
        assertEquals("skipped " + photo + ": no EXIF DateTimeOriginal" + System.lineSeparator(), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printsUsageWithoutFiles() {
        assertEquals(2, run());
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("usage:"));
    }

    @Test
    void printsUsageForAnOptionItDoesNotKnow() throws Exception {
        Path photo = copy("darktable-5.4-exif.png", "DSC_2584.png");
        assertEquals(2, run("--rnemae", photo.toString()));
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("usage:"));
        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printsAJsonReportWhenAsked() throws Exception {
        Path photo = copy("darktable-5.4-exif.png", "DSC_2584.png");
        Path blank = copy("no-exif.png", "DSC_2585.png");
        assertEquals(0, run("--json", photo.toString(), blank.toString()));
        assertEquals("{\"dryRun\":true,"
                        + "\"renamed\":[{\"from\":\"" + photo + "\",\"to\":\"" + dir.resolve("2024-12-30_23-59-58.png") + "\"}],"
                        + "\"skipped\":[{\"file\":\"" + blank + "\",\"reason\":\"no EXIF DateTimeOriginal\"}],"
                        + "\"failed\":[]}" + System.lineSeparator(),
                out.toString(StandardCharsets.UTF_8));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }
}
