package com.cwfreeman.darktable;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

/**
 * Carries out a rename plan, writing what it does to out and what goes wrong to err: prints each
 * rename, then makes it with its Move, moveFile, or DRY_RUN to make none.
 */
final class Renamer {
    private final PrintStream out;
    private final PrintStream err;

    /** Makes no rename, so it never fails. */
    static final Move DRY_RUN = (r, err) -> true;

    private final Move move;

    Renamer(PrintStream out, PrintStream err, Move move) {
        this.out = out;
        this.err = err;
        this.move = move;
    }

    /**
     * Carries out a plan: prints what it skips, then carries out each rename. 0 when every rename
     * worked, else 1.
     */
    int run(RenamePlan plan) {
        plan.skipped().forEach(skipped -> err.println("skipped " + skipped.file() + ": " + skipped.reason()));
        // reduce, not allMatch: every rename is carried out, even after one fails.
        boolean allDone = plan.renames().stream()
                .map(this::carryOut)
                .reduce(true, Boolean::logicalAnd);
        return allDone ? 0 : 1;
    }

    /** Carries out one rename: prints it, then makes it; false when it failed. */
    private boolean carryOut(RenamePlan.Rename r) {
        print(r);
        return move.apply(r, err);
    }

    /** Prints a rename: the file's path, then the name it gets. */
    private void print(RenamePlan.Rename r) {
        out.println(r.from() + " -> " + r.to().getName());
    }

    /** Moves the file; false, after writing why to err, when it could not be moved. */
    static boolean moveFile(RenamePlan.Rename r, PrintStream err) {
        try {
            Files.move(r.from().toPath(), r.to().toPath());
        } catch (IOException e) {
            err.println("could not rename " + r.from() + ": " + e);
            return false;
        }
        return true;
    }

    /** How a rename is made: true when it worked, else false after writing to err why it did not. */
    interface Move {
        boolean apply(RenamePlan.Rename r, PrintStream err);
    }
}
