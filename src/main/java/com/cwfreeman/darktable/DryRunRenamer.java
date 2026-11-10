package com.cwfreeman.darktable;

import java.io.PrintStream;

/** Prints the renames a plan would make, and makes none. */
final class DryRunRenamer extends Renamer {
    DryRunRenamer(PrintStream out, PrintStream err) {
        super(out, err);
    }

    /** Prints the rename and makes none, so it never fails. */
    @Override
    boolean carryOut(RenamePlan.Rename r) {
        print(r);
        return true;
    }
}
