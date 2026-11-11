package com.cwfreeman.darktable;

import java.io.IOException;

/** What a Renamer tells as it carries out a plan. */
interface Report {
    /** A file the plan skips, and why. */
    void skipped(RenamePlan.Skipped skipped);

    /** A rename made, or in a dry run one that would be. */
    void renamed(RenamePlan.Rename r);

    /** A rename that failed, and why. */
    void failed(RenamePlan.Rename r, IOException e);

    /** Called after the last rename. */
    default void finished() {
    }
}
