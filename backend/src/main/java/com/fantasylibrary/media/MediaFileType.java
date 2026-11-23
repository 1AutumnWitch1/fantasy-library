package com.fantasylibrary.media;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

enum MediaFileType {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    GIF("gif", "image/gif"),
    AVIF("avif", "image/avif"),
    MP4("mp4", "video/mp4"),
    WEBM("webm", "video/webm");

    private static final Set<String> MP4_BRANDS =
            Set.of("isom", "iso2", "iso3", "iso4", "iso5", "iso6", "iso7", "iso8", "iso9",
                    "mp41", "mp42", "avc1", "dash", "M4V ", "M4VH", "M4VP", "MSNV");
    private final String extension;
    private final String contentType;

    MediaFileType(String extension, String contentType) {
        this.extension = extension;
        this.contentType = contentType;
    }

    String extension() {
        return extension;
    }

    String contentType() {
        return contentType;
    }

    boolean video() {
        return this == MP4 || this == WEBM;
    }

    static MediaFileType detect(byte[] header) {
        if (header.length < 12) return null;
        if (startsWith(header, 0xff, 0xd8, 0xff)) return JPEG;
        if (startsWith(header, 0x89, 'P', 'N', 'G', 13, 10, 26, 10) && "IHDR".equals(ascii(header, 12))) return PNG;
        if ("RIFF".equals(ascii(header, 0)) && "WEBP".equals(ascii(header, 8))
                && Set.of("VP8 ", "VP8L", "VP8X").contains(ascii(header, 12))) return WEBP;
        if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8'
                && (header[4] == '7' || header[4] == '9') && header[5] == 'a') return GIF;
        if (startsWith(header, 0x1a, 0x45, 0xdf, 0xa3) && isWebm(header)) return WEBM;
        if ("ftyp".equals(ascii(header, 4))) return isoMedia(header);
        return null;
    }

    private static MediaFileType isoMedia(byte[] header) {
        long size = Integer.toUnsignedLong(ByteBuffer.wrap(header).getInt());
        if (size < 16 || size > header.length) return null;
        boolean avif = false;
        boolean mp4 = false;
        for (int index = 8; index + 4 <= size; index += 4) {
            if (index == 12) continue; // Minor version, not a brand.
            String brand = ascii(header, index);
            avif |= "avif".equals(brand) || "avis".equals(brand);
            mp4 |= MP4_BRANDS.contains(brand);
        }
        return avif ? AVIF : mp4 ? MP4 : null;
    }

    private static boolean isWebm(byte[] header) {
        Vint size = variableInteger(header, 4, false);
        if (size == null || size.value() > header.length - 4 - size.length()) return false;
        int index = 4 + size.length();
        int end = index + (int) size.value();
        while (index < end) {
            Vint id = variableInteger(header, index, true);
            if (id == null) return false;
            index += id.length();
            Vint section = variableInteger(header, index, false);
            if (section == null) return false;
            index += section.length();
            if (section.value() > end - index) return false;
            if (id.value() == 0x4282) return section.value() == 4 && "webm".equals(ascii(header, index));
            index += (int) section.value();
        }
        return false;
    }

    private static Vint variableInteger(byte[] bytes, int offset, boolean preserveMarker) {
        if (offset >= bytes.length) return null;
        int first = Byte.toUnsignedInt(bytes[offset]);
        int marker = 0x80;
        int length = 1;
        while (marker != 0 && (first & marker) == 0) {
            marker >>= 1;
            length++;
        }
        if (marker == 0 || offset + length > bytes.length) return null;
        long value = preserveMarker ? first : first & (marker - 1);
        for (int index = 1; index < length; index++) {
            value = (value << 8) | Byte.toUnsignedInt(bytes[offset + index]);
        }
        return new Vint(value, length);
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        for (int index = 0; index < signature.length; index++) {
            if (Byte.toUnsignedInt(bytes[index]) != signature[index]) return false;
        }
        return true;
    }

    private static String ascii(byte[] bytes, int offset) {
        return offset + 4 > bytes.length ? "" : new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    private record Vint(long value, int length) {}
}
