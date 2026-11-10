package com.cwfreeman.darktable;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

/** Renames the files of a plan, printing each rename. */
final class FileRenamer extends Renamer {
    FileRenamer(PrintStream out, PrintStream err) {
        super(out, err);
    }

    /** Prints the rename and makes it; false when the file could not be moved. */
    @Override
    boolean carryOut(RenamePlan.Rename r) {
        print(r);
        try {
            Files.move(r.from().toPath(), r.to().toPath());
        } catch (IOException e) {
            err.println("could not rename " + r.from() + ": " + e);
            return false;
        }
        return true;
    }
}
