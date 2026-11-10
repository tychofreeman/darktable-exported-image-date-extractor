package com.cwfreeman.darktable;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.ByteArrayReader;
import com.drew.lang.KeyValuePair;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifReader;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.png.PngDirectory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads when a photo was taken (EXIF DateTimeOriginal) from an image darktable exported. darktable 5
 * writes a PNG's EXIF as an eXIf chunk, which metadata-extractor reads itself; older versions wrote it
 * hex-encoded in a "Raw profile type exif" text chunk, which is decoded here and handed to its ExifReader.
 */
final class DateTaken {
    private static final DateTimeFormatter EXIF_DATE = DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss");

    private DateTaken() {
    }

    static Optional<LocalDateTime> read(File file) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        for (byte[] tiff : rawProfileExif(metadata)) {
            new ExifReader().extract(new ByteArrayReader(tiff), metadata);
        }
        return metadata.getDirectoriesOfType(ExifSubIFDDirectory.class).stream()
                .map(directory -> directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL))
                .filter(Objects::nonNull)
                .findFirst()
                .map(date -> LocalDateTime.parse(date.trim(), EXIF_DATE));
    }

    private static List<byte[]> rawProfileExif(Metadata metadata) {
        List<byte[]> profiles = new ArrayList<>();
        for (PngDirectory directory : metadata.getDirectoriesOfType(PngDirectory.class)) {
            @SuppressWarnings("unchecked")
            List<KeyValuePair> texts = (List<KeyValuePair>) directory.getObject(PngDirectory.TAG_TEXTUAL_DATA);
            if (texts == null) {
                continue;
            }
            for (KeyValuePair text : texts) {
                if (RawProfile.EXIF_KEYWORD.equals(text.getKey())) {
                    profiles.add(RawProfile.decode(text.getValue().toString()));
                }
            }
        }
        return profiles;
    }
}
