package com.cwfreeman.darktable;

import java.io.IOException;
import java.io.PrintStream;

/** Reports as lines of text: each rename to out as it is made, what is skipped or fails to err. */
final class TextReport implements Report {
    private final PrintStream out;
    private final PrintStream err;

    TextReport(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /** Prints a file skipped, and why. */
    @Override
    public void skipped(RenamePlan.Skipped skipped) {
        err.println("skipped " + skipped.file() + ": " + skipped.reason());
    }

    /** Prints a rename: the file's path, then the name it gets. */
    @Override
    public void renamed(RenamePlan.Rename r) {
        out.println(r.from() + " -> " + r.to().getName());
    }

    /** Prints a rename that failed, and why. */
    @Override
    public void failed(RenamePlan.Rename r, IOException e) {
        renamed(r);
        err.println("could not rename " + r.from() + ": " + e);
    }
}
