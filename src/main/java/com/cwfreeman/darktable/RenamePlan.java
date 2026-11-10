package com.cwfreeman.darktable;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Which files to rename to the date they were taken (2024-12-30_23-59-58.png), worked out before any is
 * renamed. Photos taken in the same second, or whose name is already used, get _2, _3, ... after the
 * date. A file already named for its date keeps its name; one without a date is skipped, with the reason.
 */
record RenamePlan(List<Rename> renames, List<Skipped> skipped) {
    record Rename(File from, File to) {
    }

    record Skipped(File file, String reason) {
    }

    private static final DateTimeFormatter NAME = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss");

    static RenamePlan of(List<File> files) {
        List<Skipped> skipped = new ArrayList<>();
        Map<File, LocalDateTime> dated = new LinkedHashMap<>();
        for (File file : files) {
            try {
                Optional<LocalDateTime> taken = DateTaken.read(file);
                if (taken.isPresent()) {
                    dated.put(file, taken.get());
                } else {
                    skipped.add(new Skipped(file, "no EXIF DateTimeOriginal"));
                }
            } catch (Exception e) {
                skipped.add(new Skipped(file, e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }

        List<Rename> renames = new ArrayList<>();
        Set<File> claimed = new HashSet<>();
        dated.forEach((file, taken) -> {
            if (isNamedFor(file, taken)) {
                claimed.add(file.getAbsoluteFile());
            }
        });
        dated.forEach((file, taken) -> {
            if (isNamedFor(file, taken)) {
                return;
            }
            File target;
            int n = 1;
            do {
                target = new File(file.getParentFile(), name(taken, n++, extension(file)));
            } while (target.exists() || claimed.contains(target.getAbsoluteFile()));
            claimed.add(target.getAbsoluteFile());
            renames.add(new Rename(file, target));
        });
        return new RenamePlan(List.copyOf(renames), List.copyOf(skipped));
    }

    private static String name(LocalDateTime taken, int n, String extension) {
        return NAME.format(taken) + (n == 1 ? "" : "_" + n) + extension;
    }

    private static boolean isNamedFor(File file, LocalDateTime taken) {
        String pattern = Pattern.quote(NAME.format(taken)) + "(_\\d+)?" + Pattern.quote(extension(file));
        return file.getName().matches(pattern);
    }

    /** The extension with its dot, as it is (.PNG stays .PNG), or "" when the name has none. */
    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot) : "";
    }
}
