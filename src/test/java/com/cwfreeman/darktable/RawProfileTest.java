package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawProfileTest {
    private static final byte[] TIFF_HEADER = {0x49, 0x49, 0x2a, 0x00};

    @Test
    void decodesTheHexLinesAfterTheHeaderAndDropsTheExifPrefix() {
        String text = "\nexif\n      10\n4578696600\n0049492a00\n";
        assertArrayEquals(TIFF_HEADER, RawProfile.decode(text));
    }

    @Test
    void decodesAPayloadWithoutTheExifPrefix() {
        String text = "\nexif\n       4\n49492a00\n";
        assertArrayEquals(TIFF_HEADER, RawProfile.decode(text));
    }

    @Test
    void rejectsAPayloadShorterThanItsDeclaredLength() {
        String text = "\nexif\n      12\n49492a00\n";
        assertThrows(IllegalArgumentException.class, () -> RawProfile.decode(text));
    }

    @Test
    void rejectsTextWithoutAHeader() {
        assertThrows(IllegalArgumentException.class, () -> RawProfile.decode("49492a00"));
    }
}
