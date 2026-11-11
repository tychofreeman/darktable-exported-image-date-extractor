package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenamePlanTest {
    private static final String TAKEN = "2024-12-30_23-59-58";

    @TempDir
    Path dir;

    private File copy(String fixture, String name) throws Exception {
        return Files.copy(DateTakenTest.fixture(fixture).toPath(), dir.resolve(name)).toFile();
    }

    private File in(String name) {
        return dir.resolve(name).toFile();
    }

    @Test
    void namesAFileAfterTheDateItWasTaken() throws Exception {
        File photo = copy("raw-profile-exif.png", "DSC_2584.png");
        RenamePlan plan = RenamePlan.of(List.of(photo));
        assertEquals(List.of(new RenamePlan.Rename(photo, in(TAKEN + ".png"))), plan.renames());
        assertEquals(List.of(), plan.skipped());
    }

    @Test
    void keepsTheExtensionAsItIs() throws Exception {
        File photo = copy("darktable-5.4-exif.png", "DSC_2584.PNG");
        assertEquals(List.of(new RenamePlan.Rename(photo, in(TAKEN + ".PNG"))), RenamePlan.of(List.of(photo)).renames());
    }

    @Test
    void numbersPhotosTakenInTheSameSecond() throws Exception {
        File first = copy("raw-profile-exif.png", "DSC_1.png");
        File second = copy("darktable-5.4-exif.png", "DSC_2.png");
        assertEquals(List.of(
                        new RenamePlan.Rename(first, in(TAKEN + ".png")),
                        new RenamePlan.Rename(second, in(TAKEN + "_2.png"))),
                RenamePlan.of(List.of(first, second)).renames());
    }

    @Test
    void doesNotOverwriteAFileAlreadyThere() throws Exception {
        copy("no-exif.png", TAKEN + ".png");
        File photo = copy("raw-profile-exif.png", "DSC_2584.png");
        assertEquals(List.of(new RenamePlan.Rename(photo, in(TAKEN + "_2.png"))), RenamePlan.of(List.of(photo)).renames());
    }

    @Test
    void leavesAFileAlreadyNamedForItsDate() throws Exception {
        File named = copy("raw-profile-exif.png", TAKEN + ".png");
        File photo = copy("darktable-5.4-exif.png", "DSC_2584.png");
        RenamePlan plan = RenamePlan.of(List.of(named, photo));
        assertEquals(List.of(new RenamePlan.Rename(photo, in(TAKEN + "_2.png"))), plan.renames());
        assertEquals(List.of(), plan.skipped());
    }

    @Test
    void skipsAFileWithoutADateAndSaysWhy() throws Exception {
        File photo = copy("no-exif.png", "DSC_2584.png");
        RenamePlan plan = RenamePlan.of(List.of(photo));
        assertEquals(List.of(), plan.renames());
        assertEquals(List.of(new RenamePlan.Skipped(photo, "no EXIF DateTimeOriginal")), plan.skipped());
    }

    @Test
    void skipsAFileItCannotReadAndSaysWhy() throws Exception {
        File photo = Files.writeString(dir.resolve("notes.png"), "not an image").toFile();
        RenamePlan plan = RenamePlan.of(List.of(photo));
        assertEquals(List.of(), plan.renames());
        assertEquals(1, plan.skipped().size());
        assertEquals(photo, plan.skipped().get(0).file());
    }

    @Test
    void takesTheFirstNameNoFileHas() {
        File photo = new File("photos", "DSC_2584.png");
        LocalDateTime taken = LocalDateTime.of(2024, 12, 30, 23, 59, 58);
        Set<File> there = Set.of(new File("photos", TAKEN + ".png"), new File("photos", TAKEN + "_2.png"));
        assertEquals(List.of(new RenamePlan.Rename(photo, new File("photos", TAKEN + "_3.png"))),
                RenamePlan.renamesFor(Map.of(photo, taken), there::contains));
    }
}
