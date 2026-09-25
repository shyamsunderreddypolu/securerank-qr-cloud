package com.securerank.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.securerank.service.QRVisualCryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class QRVisualCryptoServiceImpl implements QRVisualCryptoService {

    private static final int BLACK_RGB = Color.BLACK.getRGB();
    private static final int WHITE_RGB = Color.WHITE.getRGB();

    // 2x2 subpixel patterns with exactly 2 black and 2 white subpixels
    // Representation: [top-left, top-right, bottom-left, bottom-right] (1 = black, 0 = white)
    private static final int[][] PATTERNS = {
            {1, 1, 0, 0},
            {0, 0, 1, 1},
            {1, 0, 1, 0},
            {0, 1, 0, 1},
            {1, 0, 0, 1},
            {0, 1, 1, 0}
    };

    @Override
    public BufferedImage generateQRCode(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? BLACK_RGB : WHITE_RGB);
                }
            }
            return image;
        } catch (Exception e) {
            log.error("Failed to generate QR Code: {}", e.getMessage(), e);
            throw new RuntimeException("QR Code generation error: " + e.getMessage());
        }
    }

    @Override
    public String imageToBase64DataUri(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.error("Failed to convert image to Base64: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public Map<String, Object> generateVisualCryptoShares(BufferedImage qrImage) {
        int origW = qrImage.getWidth();
        int origH = qrImage.getHeight();

        // 2x2 expansion: Share width = 2 * origW, Share height = 2 * origH
        int shareW = origW * 2;
        int shareH = origH * 2;

        BufferedImage share1 = new BufferedImage(shareW, shareH, BufferedImage.TYPE_INT_RGB);
        BufferedImage share2 = new BufferedImage(shareW, shareH, BufferedImage.TYPE_INT_RGB);

        Random random = new Random();
        StringBuilder bitStreamBuilder = new StringBuilder(shareW * shareH);

        for (int y = 0; y < origH; y++) {
            for (int x = 0; x < origW; x++) {
                int rgb = qrImage.getRGB(x, y);
                // In RGB, black is close to 0, white is close to -1 (0xFFFFFF)
                boolean isBlack = (rgb & 0xFF) < 128;

                int patternIdx = random.nextInt(PATTERNS.length);
                int[] p1 = PATTERNS[patternIdx];
                int[] p2;

                if (isBlack) {
                    // For black pixel: Share 2 gets complementary pattern
                    p2 = new int[]{1 - p1[0], 1 - p1[1], 1 - p1[2], 1 - p1[3]};
                } else {
                    // For white pixel: Share 2 gets identical pattern
                    p2 = new int[]{p1[0], p1[1], p1[2], p1[3]};
                }

                int sx = x * 2;
                int sy = y * 2;

                // Subpixel 0: top-left
                share1.setRGB(sx, sy, p1[0] == 1 ? BLACK_RGB : WHITE_RGB);
                share2.setRGB(sx, sy, p2[0] == 1 ? BLACK_RGB : WHITE_RGB);

                // Subpixel 1: top-right
                share1.setRGB(sx + 1, sy, p1[1] == 1 ? BLACK_RGB : WHITE_RGB);
                share2.setRGB(sx + 1, sy, p2[1] == 1 ? BLACK_RGB : WHITE_RGB);

                // Subpixel 2: bottom-left
                share1.setRGB(sx, sy + 1, p1[2] == 1 ? BLACK_RGB : WHITE_RGB);
                share2.setRGB(sx, sy + 1, p2[2] == 1 ? BLACK_RGB : WHITE_RGB);

                // Subpixel 3: bottom-right
                share1.setRGB(sx + 1, sy + 1, p1[3] == 1 ? BLACK_RGB : WHITE_RGB);
                share2.setRGB(sx + 1, sy + 1, p2[3] == 1 ? BLACK_RGB : WHITE_RGB);
            }
        }

        // Flatten Share 1 into binary bit stream ('1' for Black, '0' for White)
        for (int y = 0; y < shareH; y++) {
            for (int x = 0; x < shareW; x++) {
                int rgb = share1.getRGB(x, y);
                bitStreamBuilder.append((rgb & 0xFF) < 128 ? '1' : '0');
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("share1", share1);
        result.put("share2", share2);
        result.put("share1Base64", imageToBase64DataUri(share1));
        result.put("share2Base64", imageToBase64DataUri(share2));
        result.put("bitStream", bitStreamBuilder.toString());
        result.put("bitStreamLength", bitStreamBuilder.length());
        result.put("shareWidth", shareW);
        result.put("shareHeight", shareH);

        log.info("Generated 2-out-of-2 Visual Cryptography shares: {}x{} subpixels, bitstream length: {}",
                shareW, shareH, bitStreamBuilder.length());

        return result;
    }

    @Override
    public BufferedImage superimposeShares(BufferedImage share1, BufferedImage share2) {
        int width = Math.min(share1.getWidth(), share2.getWidth());
        int height = Math.min(share1.getHeight(), share2.getHeight());

        BufferedImage stacked = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean s1Black = (share1.getRGB(x, y) & 0xFF) < 128;
                boolean s2Black = (share2.getRGB(x, y) & 0xFF) < 128;

                // In Visual Cryptography (OR superposition), any black subpixel turns the combined subpixel black
                if (s1Black || s2Black) {
                    stacked.setRGB(x, y, BLACK_RGB);
                } else {
                    stacked.setRGB(x, y, WHITE_RGB);
                }
            }
        }
        return stacked;
    }

    @Override
    public String extractBinaryBitStream(BufferedImage shareImage) {
        int w = shareImage.getWidth();
        int h = shareImage.getHeight();
        StringBuilder sb = new StringBuilder(w * h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = shareImage.getRGB(x, y);
                sb.append((rgb & 0xFF) < 128 ? '1' : '0');
            }
        }
        return sb.toString();
    }

    @Override
    public BufferedImage reconstructImageFromBitStream(String bitStream, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int idx = 0;
        int len = bitStream.length();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (idx < len) {
                    char bit = bitStream.charAt(idx++);
                    image.setRGB(x, y, bit == '1' ? BLACK_RGB : WHITE_RGB);
                } else {
                    image.setRGB(x, y, WHITE_RGB);
                }
            }
        }
        return image;
    }
}
