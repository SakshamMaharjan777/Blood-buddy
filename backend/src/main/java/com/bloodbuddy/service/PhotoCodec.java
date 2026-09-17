package com.bloodbuddy.service;

import java.util.Base64;

/**
 * §2.5 image plumbing: the profile/register pages hand the API a Base64 data URL
 * ("data:image/png;base64,iVBORw0…"), and the database stores raw bytes in
 * {@code donors.photo} (MEDIUMBLOB). Both directions live here so no service
 * re-implements the unwrapping.
 *
 * <p>The content type is NOT stored: the ERD has one BLOB column, so on the way
 * out the bytes are re-wrapped by sniffing their magic number (PNG → JPEG →
 * generic octet-stream). That keeps the image renderable in an
 * {@code <img src="data:…">} without a second column.
 */
public final class PhotoCodec {

    private static final String PNG_PREFIX = "data:image/png;base64,";
    private static final String JPEG_PREFIX = "data:image/jpeg;base64,";

    private PhotoCodec() {
    }

    /**
     * data URL → raw bytes.
     *
     * @throws BusinessException when the payload is not a Base64 PNG/JPEG data
     *         URL, or is empty — never stores a malformed blob that only fails
     *         when a browser tries to render it.
     */
    public static byte[] decode(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank()) {
            throw new BusinessException("No image supplied.");
        }
        String payload;
        if (dataUrl.startsWith(PNG_PREFIX)) {
            payload = dataUrl.substring(PNG_PREFIX.length());
        } else if (dataUrl.startsWith(JPEG_PREFIX)) {
            payload = dataUrl.substring(JPEG_PREFIX.length());
        } else {
            throw new BusinessException("Photos must be a PNG or JPEG data URL.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            if (bytes.length == 0) {
                throw new BusinessException("The image is empty.");
            }
            return bytes;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("The image is not valid Base64.");
        }
    }

    /** raw bytes → data URL with a sniffed content type (null when there is no photo). */
    public static String encode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return "data:" + contentType(bytes) + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /** The sniffed media type of stored bytes (the B3 photo endpoint sets it on the response). */
    public static String contentType(byte[] bytes) {
        if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "image/png";
        }
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
