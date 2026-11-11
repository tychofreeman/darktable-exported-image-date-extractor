package com.cwfreeman.darktable;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

/**
 * Carries out a rename plan, writing what it does to out and what goes wrong to err. On its own a
 * dry run: it prints each rename and makes none; FileRenamer makes them.
 */
class Renamer {
    final PrintStream out;
    final PrintStream err;

    Renamer(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Carries out the plan for files: prints what it skips, then carries out each rename. 0 when
     * every rename worked, else 1.
     */
    int run(List<File> files) {
        RenamePlan plan = RenamePlan.of(files);
        plan.skipped().forEach(skipped -> err.println("skipped " + skipped.file() + ": " + skipped.reason()));
        boolean allDone = plan.renames().stream()
                .map(this::carryOut)
                .reduce(true, Boolean::logicalAnd);
        return allDone ? 0 : 1;
    }

    /** Carries out one rename: prints it, then makes it; false when it failed. */
    boolean carryOut(RenamePlan.Rename r) {
        print(r);
        return move(r);
    }

    /** Makes the rename; false when it failed. A dry run: makes none, so it never fails. */
    boolean move(RenamePlan.Rename r) {
        return true;
    }

    /** Prints a rename: the file's path, then the name it gets. */
    void print(RenamePlan.Rename r) {
        out.println(r.from() + " -> " + r.to().getName());
    }
}
