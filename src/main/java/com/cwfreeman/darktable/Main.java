package com.cwfreeman.darktable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Renames images darktable exported to the date they were taken (EXIF DateTimeOriginal), so they sort
 * in the order they were shot. Prints the renames it would make; renames only when given --rename.
 */
public class Main {
    private static final String USAGE = String.join(System.lineSeparator(),
            "usage: darktable-date-namer [--rename] <image or directory>...",
            "  Prints how each image would be renamed to the date it was taken (2024-12-30_23-59-58.png);",
            "  with --rename, renames them. A directory stands for the images directly in it.");

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "tif", "tiff", "webp");

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        List<String> paths = new ArrayList<>(List.of(args));
        boolean rename = paths.remove("--rename");
        if (paths.isEmpty()) {
            err.println(USAGE);
            return 2;
        }
        List<File> files = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (file.isDirectory()) {
                files.addAll(imagesIn(file));
            } else {
                files.add(file);
            }
        }

        RenamePlan plan = RenamePlan.of(files);
        for (RenamePlan.Skipped skipped : plan.skipped()) {
            err.println("skipped " + skipped.file() + ": " + skipped.reason());
        }
        int status = 0;
        for (RenamePlan.Rename r : plan.renames()) {
            out.println(r.from() + " -> " + r.to().getName());
            if (rename) {
                try {
                    Files.move(r.from().toPath(), r.to().toPath());
                } catch (IOException e) {
                    err.println("could not rename " + r.from() + ": " + e);
                    status = 1;
                }
            }
        }
        return status;
    }

    private static List<File> imagesIn(File directory) {
        File[] files = directory.listFiles(file -> file.isFile() && isImage(file));
        if (files == null) {
            return List.of();
        }
        Arrays.sort(files);
        return List.of(files);
    }

    private static boolean isImage(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
