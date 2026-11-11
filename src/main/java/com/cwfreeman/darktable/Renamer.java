package com.cwfreeman.darktable;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Carries out a rename plan: makes each rename with its Move, moveFile, or DRY_RUN to make none,
 * and tells its Report what it renamed, skipped and failed to rename.
 */
final class Renamer {
    private final Report report;

    /** Makes no rename. */
    static final Move DRY_RUN = r -> {
    };

    private final Move move;

    Renamer(Report report, Move move) {
        this.report = report;
        this.move = move;
    }

    /**
     * Carries out a plan: reports what it skips, then carries out each rename. 0 when every rename
     * worked, else 1.
     */
    int run(RenamePlan plan) {
        plan.skipped().forEach(report::skipped);
        // reduce, not allMatch: every rename is carried out, even after one fails.
        boolean allDone = plan.renames().stream()
                .map(this::carryOut)
                .reduce(true, Boolean::logicalAnd);
        report.finished();
        return allDone ? 0 : 1;
    }

    /** Carries out one rename, then reports it; false when it failed. */
    private boolean carryOut(RenamePlan.Rename r) {
        try {
            move.apply(r);
        } catch (IOException e) {
            report.failed(r, e);
            return false;
        }
        report.renamed(r);
        return true;
    }

    /** Moves the file. */
    static void moveFile(RenamePlan.Rename r) throws IOException {
        Files.move(r.from().toPath(), r.to().toPath());
    }

    /** How a rename is made. */
    interface Move {
        void apply(RenamePlan.Rename r) throws IOException;
    }
}
