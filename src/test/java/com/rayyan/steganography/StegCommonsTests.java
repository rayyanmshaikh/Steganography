package com.rayyan.steganography;

import com.rayyan.steganography.services.ServicesCommons;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static com.rayyan.steganography.TestUtils.createInputFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class StegCommonsTests {

    private static final String normalImg = "/images/normal.jpg";

    /**
     * Tests that input image verification succeeds for a valid image file.
     *
     * @throws IOException if the image cannot be read or validated
     */
    @Test
    public void testInputVerificationSuccess() throws IOException {
        MultipartFile multipartFile = createInputFile(normalImg);
        ServicesCommons.verifyInput(multipartFile);
    }

    /**
     * Tests the calculation of the maximum number of storable characters in an
     * image.
     * Verifies that the computed value matches the expected value based on image
     * dimensions.
     *
     * @throws IOException if the image cannot be read
     */
    @Test
    public void testMaxStorableChars() throws IOException {
        MultipartFile multipartFile = createInputFile(normalImg);

        BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
        int expectedPixels = bufferedImage.getHeight() * bufferedImage.getWidth();
        int expectedChars = ((expectedPixels * 3) / 8) - 12;

        assertEquals(expectedChars, ServicesCommons.getMaxStorableChars(multipartFile),
                "Max storable chars did not match expected value.");
    }
}
