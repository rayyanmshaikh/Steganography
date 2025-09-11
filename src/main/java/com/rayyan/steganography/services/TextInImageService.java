package com.rayyan.steganography.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.rayyan.steganography.services.ServicesCommons.convertTextToBytes;
import static com.rayyan.steganography.services.ServicesCommons.verifyInput;

@Service
public class TextInImageService implements StegService {

    private static final String MAGIC_HEADER = "STEG";

    private static final Logger logger = LoggerFactory.getLogger(TextInImageService.class);

    @Override
    public String getType() {
        return "text-in-image";
    }

    @Override
    public ResponseEntity<?> encode(MultipartFile carrier, String text) {
        try {
            verifyInput(carrier, text);

            final BufferedImage orig = ImageIO.read(carrier.getInputStream());

            if (orig == null) {
                logger.error("Input image could not be read.");
                return ResponseEntity.badRequest().body("Input image could not be read.");
            }

            final BufferedImage img = new BufferedImage(orig.getWidth(), orig.getHeight(), BufferedImage.TYPE_INT_RGB);
            img.getGraphics().drawImage(orig, 0, 0, null);

            final BufferedImage encoded = getEncodedImage(img, text);
            logger.info("Writing encoded image");

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(encoded, "png", baos);
                final byte[] imageBytes = baos.toByteArray();

                final HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.IMAGE_PNG);
                headers.setContentLength(imageBytes.length);
                headers.setContentDispositionFormData("attachment", "encoded.png");

                logger.info("Sending response");
                return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

            } catch (IOException e) {
                logger.error("Failed to write encoded image", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to write image: " + e.getMessage());
            }

        } catch (IOException e) {
            logger.error("Encoding failed due to IO error", e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error during encoding", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Encoding failed: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> decode(MultipartFile carrier) {
        try {
            verifyInput(carrier);

            final BufferedImage img = ImageIO.read(carrier.getInputStream());
            if (img == null) {
                logger.error("Input image could not be read.");
                return ResponseEntity.badRequest().body("Input image could not be read.");
            }

            final String decoded = getDecodedImage(img).replace("\0", "");
            if (!decoded.startsWith(MAGIC_HEADER)) {
                logger.info("No encoded message found");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No hidden text found");
            }

            logger.info("Returning decoded message");
            final String message = decoded.substring(MAGIC_HEADER.length());

            return ResponseEntity.ok(message);

        } catch (IOException e) {
            logger.error("Decoding failed due to IO error", e);
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error during decoding", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Decoding failed: " + e.getMessage());
        }
    }

    private static BufferedImage getEncodedImage(BufferedImage img, String text) {
        logger.info("Encoding image");
        StringBuilder bits = convertTextToBytes(MAGIC_HEADER + text);

        int msgIdx = 0;
        final int height = img.getHeight();
        final int width = img.getWidth();
        final BufferedImage encoded = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        outer: for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                if (msgIdx < bits.length()) {
                    r = (r & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                if (msgIdx < bits.length()) {
                    g = (g & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                if (msgIdx < bits.length()) {
                    b = (b & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                encoded.setRGB(x, y, (r << 16) | (g << 8) | b);

                if (msgIdx >= bits.length()) {
                    for (int i = x + 1; i < width; i++) {
                        encoded.setRGB(i, y, img.getRGB(i, y));
                    }

                    for (int j = y + 1; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            encoded.setRGB(i, j, img.getRGB(i, j));
                        }
                    }

                    break outer;
                }
            }
        }

        logger.info("Returning encoded image");
        return encoded;
    }

    /**
     * Extracts hidden text from the given steganographic image using LSB decoding.
     *
     * @param img BufferedImage object with an embedded hidden message.
     * @return The decoded text message from the image.
     */
    private static String getDecodedImage(BufferedImage img) {
        logger.info("Decoding encoded image");
        StringBuilder bits = new StringBuilder();

        outer: for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                bits.append(r & 1);
                if (checkTerminator(bits)) break outer;

                bits.append(g & 1);
                if (checkTerminator(bits)) break outer;

                bits.append(b & 1);
                if (checkTerminator(bits)) break outer;
            }
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < bits.length() - 8; i += 8) {
            int charCode = Integer.parseInt(bits.substring(i, i + 8), 2);
            message.append((char) charCode);
        }

        logger.info("Returning decoded message");
        return message.toString();
    }

    /**
     * Checks if the last 8 bits in the binary string represent the null character terminator.
     *
     * @param bits StringBuilder containing binary bits of the message.
     * @return True if the last 8 bits are all zeros, indicating end of message; otherwise false.
     */
    private static boolean checkTerminator(StringBuilder bits) {
        return bits.length() >= 8 && bits.substring(bits.length() - 8).equals("00000000");
    }
}
