package com.cwfreeman.darktable;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes the text of a PNG "Raw profile type exif" chunk, as exiv2 writes it (and older darktable
 * exports with it): a newline, the profile name, a newline, the payload's length in bytes, a newline,
 * then the payload as lines of hex. The payload may start with the JPEG APP1 "Exif\0\0" prefix,
 * which is dropped, so what is returned starts at the TIFF header.
 */
final class RawProfile {
    static final String EXIF_KEYWORD = "Raw profile type exif";

    private static final Pattern HEADER = Pattern.compile("\\A\\n?\\w+\\n *(\\d+)\\n");
    private static final byte[] EXIF_PREFIX = {'E', 'x', 'i', 'f', 0, 0};

    private RawProfile() {
    }

    static byte[] decode(String text) {
        Matcher header = HEADER.matcher(text);
        if (!header.lookingAt()) {
            throw new IllegalArgumentException("Not a raw profile: it has no header");
        }
        int length = Integer.parseInt(header.group(1));
        String hex = text.substring(header.end()).replaceAll("\\s", "");
        if (hex.length() != 2 * length) {
            throw new IllegalArgumentException(String.format(
                    "Raw profile declares %d bytes but holds %d hex digits", length, hex.length()));
        }
        byte[] payload = HexFormat.of().parseHex(hex);
        return startsWithExifPrefix(payload)
                ? Arrays.copyOfRange(payload, EXIF_PREFIX.length, payload.length)
                : payload;
    }

    private static boolean startsWithExifPrefix(byte[] payload) {
        return payload.length >= EXIF_PREFIX.length
                && Arrays.equals(payload, 0, EXIF_PREFIX.length, EXIF_PREFIX, 0, EXIF_PREFIX.length);
    }
}
