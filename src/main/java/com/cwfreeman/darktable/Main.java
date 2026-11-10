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
import java.util.stream.Stream;

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
        List<File> files = paths.stream().map(File::new).flatMap(Main::filesFor).toList();

        RenamePlan plan = RenamePlan.of(files);
        plan.skipped().forEach(skipped -> err.println("skipped " + skipped.file() + ": " + skipped.reason()));
        boolean allDone = plan.renames().stream()
                .map(r -> carryOut(r, rename, out, err))
                .reduce(true, Boolean::logicalAnd);
        return allDone ? 0 : 1;
    }

    /** Prints a rename and, when asked to, makes it; false when the rename failed. */
    private static boolean carryOut(RenamePlan.Rename r, boolean rename, PrintStream out, PrintStream err) {
        out.println(r.from() + " -> " + r.to().getName());
        if (rename) {
            try {
                Files.move(r.from().toPath(), r.to().toPath());
            } catch (IOException e) {
                err.println("could not rename " + r.from() + ": " + e);
                return false;
            }
        }
        return true;
    }

    private static Stream<File> imagesIn(File directory) {
        File[] files = directory.listFiles(file -> file.isFile() && isImage(file));
        return files == null ? Stream.empty() : Arrays.stream(files).sorted();
    }

    private static boolean isImage(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 && IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /** The images a path stands for: those directly in it when it is a directory, or else the file itself. */
    private static Stream<File> filesFor(File file) {
        return file.isDirectory() ? imagesIn(file) : Stream.of(file);
    }
}
