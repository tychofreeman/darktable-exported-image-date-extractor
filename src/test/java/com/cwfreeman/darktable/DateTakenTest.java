package com.cwfreeman.darktable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTakenTest {
    static File fixture(String name) throws URISyntaxException {
        return new File(DateTakenTest.class.getResource("/" + name).toURI());
    }

    @ParameterizedTest
    @ValueSource(strings = {"darktable-5.4-exif.png", "raw-profile-exif.png", "raw-profile-exif-big-endian.png"})
    void readsDateTimeOriginal(String name) throws Exception {
        assertEquals(Optional.of(LocalDateTime.of(2024, 12, 30, 23, 59, 58)), DateTaken.read(fixture(name)));
    }

    @Test
    void isEmptyForAnImageWithoutExif() throws Exception {
        assertEquals(Optional.empty(), DateTaken.read(fixture("no-exif.png")));
    }

    @Test
    void readsTheSubSecondsOfSubSecTimeOriginal() throws Exception {
        assertEquals(Optional.of(LocalDateTime.of(2024, 12, 30, 23, 59, 58, 123_000_000)),
                DateTaken.read(fixture("darktable-5.4-subsec.png")));
    }
}
