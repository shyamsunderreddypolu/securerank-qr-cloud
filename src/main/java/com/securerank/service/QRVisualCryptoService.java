package com.securerank.service;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface QRVisualCryptoService {

    /**
     * Generates a QR Code as a BufferedImage for the given content payload.
     */
    BufferedImage generateQRCode(String content, int width, int height);

    /**
     * Converts a BufferedImage to a Base64-encoded PNG data URI string.
     */
    String imageToBase64DataUri(BufferedImage image);

    /**
     * Splits a QR code image into 2 visual cryptographic shares using a (2,2) VC scheme.
     * Returns a map containing:
     * - "share1": BufferedImage
     * - "share2": BufferedImage
     * - "share1Base64": String
     * - "share2Base64": String
     * - "bitStream": String ('0' and '1' binary stream)
     */
    Map<String, Object> generateVisualCryptoShares(BufferedImage qrImage);

    /**
     * Reconstructs the secret image by superimposing / stacking Share 1 and Share 2.
     */
    BufferedImage superimposeShares(BufferedImage share1, BufferedImage share2);

    /**
     * Flattens a binary share image into a binary bit stream string ("010101...").
     */
    String extractBinaryBitStream(BufferedImage shareImage);

    /**
     * Reconstructs a BufferedImage from a binary bit stream.
     */
    BufferedImage reconstructImageFromBitStream(String bitStream, int width, int height);
}
