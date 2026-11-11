package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonReportTest {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    @Test
    void listsAFailureWithItsErrorEscapingWhatJsonMust() {
        JsonReport report = new JsonReport(new PrintStream(out, true, StandardCharsets.UTF_8), false);
        report.failed(new RenamePlan.Rename(new File("a\"b\\c.png"), new File("d.png")), new IOException("gone\n\u0001"));
        report.finished();
        assertEquals("{\"dryRun\":false,\"renamed\":[],\"skipped\":[],\"failed\":[{\"from\":\"a\\\"b\\\\c.png\",\"to\":\"d.png\","
                        + "\"error\":\"java.io.IOException: gone\\n\\u0001\"}]}" + System.lineSeparator(),
                out.toString(StandardCharsets.UTF_8));
    }
}
