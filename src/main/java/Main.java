import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.KeyValuePair;
import com.drew.metadata.Metadata;
import com.drew.metadata.png.PngDirectory;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Main {
    public static <T> Stream<T> stream(Iterable<T> in) {
        return StreamSupport.stream(in.spliterator(), false);
    }

    public static String c(byte b) {
        switch (b) {
            case 10: return "<LF>";
            case 32: return "<SPACE>";
            default:
                if (b>32) return new String(new byte[]{b});
                else return b + "";
        }
    }

    static class Field {



        enum Type {
            ASCII(1),
            BYTE(1),
            SHORT(2),
            LONG(4),
            RATIONAL(8),
            UNDEFINED(8),
            SLONG(4),
            SRATIONAL(8),
            OTHER(0);

            private final int width;

            Type(int width) {
                this.width = width;
            }
            public int totalWidth(int count) {
                int i = this.width * count;
                return i - (this == ASCII ? 1 : 0);
            }

            public static Type of(int value) {
                switch (value) {
                    case 1: return BYTE;
                    case 2: return ASCII;
                    case 3: return SHORT;
                    case 4: return LONG;
                    case 5: return RATIONAL;
                    case 7: return UNDEFINED;
                    case 9: return SLONG;
                    case 10: return SRATIONAL;
                    default: return OTHER;
                }
            }

            public String valueAsString(byte[] bytes, int count, int value) {
                switch (this) {
                    case ASCII:
                    case BYTE:
                        if (value + count > bytes.length) return String.format("COULD NOT COPY %d,%d from byte[%d]", value, count, bytes.length);
                        return new String(Arrays.copyOfRange(bytes, value, value + this.totalWidth(count)));
                    case SLONG:
                        return String.format("%d", value);
                    case SHORT:
                    case LONG:
                        return String.format("%d", value);
                    default:
                        return "";
                }
            }
        }

        String name;
        public int tag;
        Type type;
        int offset;
        final int count;
        int length;
        String value;
        String asString;

        public Field(byte[] bytes, int startOffset) {
            String tagAsString = simpleFieldHex(Arrays.copyOfRange(bytes, startOffset, startOffset + 2));;
            this.tag = interpretBytes(bytes, startOffset, 2);
            this.name = tagName(tag);
            this.type = Type.of(interpretBytes(bytes, startOffset + 2, 2));
            this.count = interpretBytes(bytes, startOffset + 4, 4);
            this.length = this.type.totalWidth(this.count);
            this.offset = interpretBytes(bytes, startOffset + 8, 4);
            this.value = this.type.valueAsString(bytes, this.count, this.offset);

            this.asString = String.format("Offset:%04x %20.20s [%s %x] [%s] [%05d] [%05d] [%s]", startOffset, this.name, tagAsString, tag, type, this.length, this.offset, this.value);
        }

        private String tagName(int tag) {
            switch (tag) {
                case 0x010E: return "ImageDescription";
                case 0x010F: return "Make";
                case 0x0110: return "Model";
                case 0x0112: return "Orientation";
                case 0x011A: return "XResolution";
                case 0x011B: return "YResolution";
                case 0x0128: return "ResolutionUnit";
                case 0x0132: return "DateTime";
                case 0x0213: return "YCbCrPositioning";
                case 0x8298: return "CopyRight";
                case 0x8769: return "Exif IFD Pointer";
                case 0x829A: return "ExposureTime";
                case 0x829D: return "FNumber";
                case 0x9000: return "ExifVersion";
                case 0x9003: return "DateTimeOriginal";
                case 0x9004: return "DateTimeDigitized";
                case 0x9101: return "ComponentsConfiguration";
                case 0x9102: return "CompressedBitsPerPixel";
                case 0x9201: return "ShutterSpeedValue";
                case 0x9202: return "ApertureValue";
                case 0x9203: return "BrightnessValue";
                case 0x9204: return "ExposureBiasValue";
                case 0x9205: return "MaxApertureRatioValue";
                case 0x9206: return "SubjectDistance";
                case 0x9207: return "MeteringMode";
                case 0x9208: return "LightSource";
                case 0x9209: return "Flash";
                case 0x920A: return "FocalLength";
                case 0x9286: return "UserComments";
                case 0x9291: return "SubSecTimeOriginal";
                case 0x9292: return "SubSecTimeDigitized";
                case 0x00A0: return "FlashpixVersion";
                case 0x01A0: return "Colorspace";
                case 0x02A0: return "Pixel X Dimension";
                case 0x03A0: return "Pixel Y Dimension";
            }
            return "???";
        }

        private int interpretBytes(byte[] bytes, int startOffset, int length) {
            int acc = 0;
            int m = 1;
            for (byte b : Arrays.copyOfRange(bytes, startOffset, startOffset + length)) {
                int bi = b;
                if (bi < 0) {
                    bi += 256;
                }
                acc += bi * m;
                m *= 256;
            }
            return acc;
        }

        public String toString() {
            return this.asString;
        }
    }

    public static String extractDate(byte[] data) {
        String exifRawData = new String(data).substring(15 + 12, 21 + 24994 + 16).replaceAll("\n", "");
        byte[] bytes = DatatypeConverter.parseHexBinary(exifRawData);

//        printStr("Byte Order", bytes, 0, 2);
//        printStr("\"42\"", bytes, 2, 2);
//        printStr("0th IFD Offset ", bytes, 4, 4);
//        printStr("Number of Interoperability", bytes, 0x8, 2);
        int offset = 0xA;
        Field f = null;
        do {
            f = new Field(bytes, offset);
            offset += 12;
        } while (offset < bytes.length - 12 && f.tag != 0x8769);
        offset = f.offset + 2;
        do {
            f = new Field(bytes, offset);
            offset += 12;
        } while (offset < bytes.length - 12 && f.tag != 0x9003);

        try {

            SimpleDateFormat inputFormat = new SimpleDateFormat("YYYY:MM:dd HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
            return String.format("%s", outputFormat.format(inputFormat.parse(f.value)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
//
//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(o);
//            fileOutputStream.write(bytes);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            new ExifReader().extract(new RandomAccessStreamReader(new ByteArrayInputStream(bytes)), metadata);
//            stream(metadata.getDirectories()).forEach(dir ->
//                    {
//                        System.out.println("\t: " + dir.getName());
//                        dir.getTags().stream().forEach(tag -> System.out.println("\t\t: " + tag.getTagName()));
//                    }
//            );
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private static void printStr(String name, byte[] bytes, int startOffset, int length) {
        System.out.println(getString(name, bytes, startOffset, length));
    }

    private static String getString(String name, byte[] bytes, int startOffset, int length) {
        byte[] bytes1 = Arrays.copyOfRange(bytes, startOffset, startOffset + length);
        String hexes = simpleFieldHex(bytes1);
        String printable = simpleFieldString(bytes1);
        return String.format("Offset:%04x %20.20s [%s] [%s]", startOffset, name, hexes, printable);
    }

    private static String simpleFieldString(byte[] bytes1) {
        String printable = new String(bytes1) + "[";
        for (byte b : bytes1) {
            if (!Character.isISOControl(b)) {
                printable += Byte.toString(b);
            } else {
                printable += "_";
            }
        }
        return printable + "]";
    }

    private static String simpleFieldHex(byte[] bytes1) {
        String hexes = "";
        for (byte b : bytes1) {
            try {
                hexes += String.format("%02X", b);
            } catch (Exception e) {
                hexes += "_";
            }
        }
        return hexes;
    }

    public static void main(String[] args) {
        File file = new File("/Users/cwfreeman/dev/DSC_2584.png");

        Arrays.asList(file).stream().forEach(f -> {
            try {
                String newName = getNewName(f);
                // Detect collisions here.
                // Then check to see if it should be uploaded.
                // If NO, but we just moved it into place, make prime name (f', f'', f''', etc)
                // Then upload if necessary.
                // Should we build a list of original name to new name mappings?
                System.out.printf("Lol %s -> %s\n", f.getName(), newName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static String getNewName(File f) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(f);
        return stream(metadata.getDirectories())
                .filter(dir -> dir.containsTag(PngDirectory.TAG_TEXTUAL_DATA))
                .map(dir -> extractDate(((ArrayList<KeyValuePair>) dir.getObject(PngDirectory.TAG_TEXTUAL_DATA)).get(0).getValue().getBytes()))
                .findFirst().map(d -> String.format("%s.png")).orElse(f.getName());
    }
}
