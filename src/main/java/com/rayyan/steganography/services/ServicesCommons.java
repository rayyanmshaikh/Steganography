package com.rayyan.steganography.services;

import org.apache.tika.Tika;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Utility class providing common validation and conversion methods for steganography services.
 * <p>
 * Includes image validation for size and MIME type, and text-to-binary conversion helpers.
 * Used by service classes to ensure consistent input handling and encoding logic.
 */
public class ServicesCommons {

    private static final ArrayList<String> mimes = new ArrayList<>(
            Arrays.asList("image/png", "image/jpeg", "image/jpg")
    );

    private static final Logger logger = LoggerFactory.getLogger(ServicesCommons.class);

    /**
     * Validates the input image for size and MIME type constraints.
     *
     * @param image Multipart image file to validate.
     * @throws IOException if the image is too large or has an unsupported MIME type.
     */
    public static void verifyInput(MultipartFile image) throws IOException {
        Tika tika = new Tika();
        if (image.getSize() > 50000000) {
            logger.warn("Image is greater than 50 MB");
            throw new FileSizeLimitExceededException("Image size is greater than 50 MB", image.getSize(), 50000000);
        } else if (!mimes.contains(tika.detect(image.getInputStream()))) {
            logger.warn("Image type not valid: {}", tika.detect(image.getInputStream()));
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
    public static void verifyInput(MultipartFile image, String text) throws IOException {
        verifyInput(image);

        if (text.length() > getMaxStorableChars(image)) {
            logger.warn("Text larger than storable chars");
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
    public static int getMaxStorableChars(MultipartFile image) throws IOException {
        BufferedImage file = ImageIO.read(image.getInputStream());

        if (file == null) {
            throw new IOException("Invalid image");
        }

        int pixels = file.getWidth() * file.getHeight();

        return ((pixels * 3) / 8) - 12;
    }

    /**
     * Converts the input message to a binary string representation with a null-terminated ending.
     *
     * @param message The input message to be converted.
     * @return A StringBuilder containing the binary representation of the message.
     */
    public static StringBuilder convertTextToBytes(String message) {
        logger.info("Converting text message to binary representation");
        message += "\0\0\0\0\0\0\0\0";

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }

        logger.info("Returning binary message");
        return binary;
    }

}
