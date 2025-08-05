package com.rayyan.steganography;

import org.apache.tika.Tika;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST controller for handling image steganography operations.
 * Provides endpoints for encoding text into an image and decoding text from an image.
 */
@RestController
@RequestMapping("/api")
public class StegController {

    private static final ArrayList<String> mimes = new ArrayList<>(
            Arrays.asList("image/png", "image/jpeg", "image/jpg")
    );

    private static final String MAGIC_HEADER = "STEG";

    /**
     * Encodes the given text into the provided image using LSB (Least Significant Bit) steganography.
     *
     * @param image Multipart image file to encode the text into.
     * @param text  The text message to hide inside the image.
     * @return A ResponseEntity containing the encoded image as a downloadable PNG file,
     *         or an error message if the input is invalid.
     * @throws IOException if an error occurs during processing.
     */
    @PostMapping(value = "/encodeTI", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> encodeTextInImage(@RequestPart("image") MultipartFile image, @RequestPart("text") String text) throws IOException {
        try {
            verifyInput(image, text);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        BufferedImage img;
        try {
            BufferedImage orig = ImageIO.read(image.getInputStream());
            img = new BufferedImage(
                    orig.getWidth(),
                    orig.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            img.getGraphics().drawImage(orig, 0, 0, null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        BufferedImage encoded = getEncodedImage(img, text);

        String contentType = image.getContentType();

        assert contentType != null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(encoded, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(imageBytes.length);
        headers.setContentDispositionFormData("attachment", "encoded.png");

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    /**
     * Decodes and retrieves hidden text from the provided steganographic image.
     *
     * @param image Multipart image file containing hidden text.
     * @return A ResponseEntity containing the decoded text,
     *         or an error message if the input is invalid.
     * @throws IOException if an error occurs during decoding.
     */
    @PostMapping(value = "/decodeTI", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> decodeTextInImage(@RequestPart("image") MultipartFile image) throws IOException {
        try {
            verifyInput(image);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        BufferedImage img;
        try {
            img = ImageIO.read(image.getInputStream());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        String decoded = getDecodedImage(img).replace("\0", "");

        if (!decoded.startsWith(MAGIC_HEADER)) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No hidden text found");
        }

        String message = decoded.substring(MAGIC_HEADER.length());
        return ResponseEntity.ok(message);
    }

    private static BufferedImage getEncodedImage(BufferedImage img, String text) {
        StringBuilder bits = convertTextToBytes(MAGIC_HEADER + text);

        int msgIdx = 0;
        int height = img.getHeight();
        int width = img.getWidth();
        BufferedImage encoded = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
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

                int newRGB = (r << 16) | (g << 8) | b;
                encoded.setRGB(x, y, newRGB);

                if (msgIdx >= bits.length()) {
                    for (int i = x + 1; i < width; i++) {
                        encoded.setRGB(i, y, img.getRGB(i, y));
                    }

                    for (int j = y + 1; j < height; j++) {
                        for (int i = 0; i < width; i++) {
                            encoded.setRGB(i, j, img.getRGB(i, j));
                        }
                    }

                    return encoded;
                }
            }
        }

        return encoded;
    }

    /**
     * Extracts hidden text from the given steganographic image using LSB decoding.
     *
     * @param img BufferedImage object with an embedded hidden message.
     * @return The decoded text message from the image.
     */
    private static String getDecodedImage(BufferedImage img) {
        StringBuilder bits = new StringBuilder();
        outer:
        for (int y = 0; y < img.getHeight(); y++) {
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

        return message.toString();
    }

    /**
     * Converts the input message to a binary string representation with a null-terminated ending.
     *
     * @param message The input message to be converted.
     * @return A StringBuilder containing the binary representation of the message.
     */
    private static StringBuilder convertTextToBytes(String message) {
        message += "\0\0\0\0\0\0\0\0";

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        return binary;
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

    /**
     * Validates the input image for size and MIME type constraints.
     *
     * @param image Multipart image file to validate.
     * @throws IOException if the image is too large or has an unsupported MIME type.
     */
    protected static void verifyInput(MultipartFile image) throws IOException {
        Tika tika = new Tika();
        if (image.getSize() > 10000000) {
            System.out.println(image.getSize());
            throw new FileSizeLimitExceededException("Image size is greater than 10 MB", image.getSize(), 10000000);
        } else if (!mimes.contains(tika.detect(image.getInputStream()))) {
            throw new FileUploadException("Image must be one of: " + mimes);
        }
    }

    /**
     * Validates both the image and the text for encoding.
     * Ensures the text can be fully stored in the image using LSB encoding.
     *
     * @param image Multipart image file to validate.
     * @param text  The text to validate against the image capacity.
     * @throws IOException if the image is invalid or the text exceeds the storable limit.
     */
    protected static void verifyInput(MultipartFile image, String text) throws IOException {
        verifyInput(image);

        if (text.length() > getMaxStorableChars(image)) {
            throw new IOException("Text length is greater than max storable chars");
        }
    }

    /**
     * Calculates the maximum number of characters that can be stored in the given image using LSB encoding.
     *
     * @param image Multipart image file used for calculating capacity.
     * @return The maximum number of characters that can be stored.
     * @throws IOException if the image is invalid or unreadable.
     */
    protected static int getMaxStorableChars(MultipartFile image) throws IOException {
        BufferedImage file = ImageIO.read(image.getInputStream());

        if (file == null) {
            throw new IOException("Invalid image");
        }

        int pixels = file.getWidth() * file.getHeight();

        return ((pixels * 3) / 8) - 12;
    }
}
