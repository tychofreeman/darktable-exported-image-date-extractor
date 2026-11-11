package com.cwfreeman.darktable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Reports as one JSON object, printed to out when the run is finished: dryRun, then the lists renamed
 * (from, to), skipped (file, reason) and failed (from, to, error).
 */
final class JsonReport implements Report {
    private final PrintStream out;
    private final boolean dryRun;
    private final List<String> renamed = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();
    private final List<String> failed = new ArrayList<>();

    JsonReport(PrintStream out, boolean dryRun) {
        this.out = out;
        this.dryRun = dryRun;
    }

    @Override
    public void skipped(RenamePlan.Skipped skipped) {
        this.skipped.add(object("file", skipped.file().toString(), "reason", skipped.reason()));
    }

    @Override
    public void renamed(RenamePlan.Rename r) {
        renamed.add(object("from", r.from().toString(), "to", r.to().toString()));
    }

    @Override
    public void failed(RenamePlan.Rename r, IOException e) {
        failed.add(object("from", r.from().toString(), "to", r.to().toString(), "error", e.toString()));
    }

    @Override
    public void finished() {
        out.println("{\"dryRun\":" + dryRun + ",\"renamed\":" + array(renamed) + ",\"skipped\":" + array(skipped)
                + ",\"failed\":" + array(failed) + "}");
    }

    private static String array(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    /** A JSON object of the names and values, in turn. */
    private static String object(String... namesAndValues) {
        StringJoiner members = new StringJoiner(",", "{", "}");
        for (int i = 0; i < namesAndValues.length; i += 2) {
            members.add(string(namesAndValues[i]) + ":" + string(namesAndValues[i + 1]));
        }
        return members.toString();
    }

    /** A JSON string: quoted, with quotes, backslashes and control characters escaped. */
    private static String string(String s) {
        StringBuilder json = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> json.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return json.append('"').toString();
    }
}
