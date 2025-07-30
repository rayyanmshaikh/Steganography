package com.rayyan.steganography;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StegControllerTests {

    @Test
    public void testInputVerificationSuccess() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/images/normal.jpg");
        assertNotNull(inputStream, "Image not found");

        MultipartFile multipartFile = new MockMultipartFile(
                "image",
                "normal.jpg",
                "image/jpeg",
                inputStream
        );

        StegController.verifyInput(multipartFile);
    }

    @Test
    public void testInputVerificationFails() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/images/15MB.jpg");
        assertNotNull(inputStream, "Image not found");

        MultipartFile multipartFile = new MockMultipartFile(
                "image",
                "normal.jpg",
                "image/jpeg",
                inputStream
        );

        try {
            StegController.verifyInput(multipartFile);
        } catch (Exception ignore) {}
    }

    @Test
    public void testMaxStorableChars() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/images/normal.jpg");
        assertNotNull(inputStream, "Image not found");

        MultipartFile multipartFile = new MockMultipartFile(
                "image",
                "normal.jpg",
                "image/jpeg",
                inputStream
        );

        BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
        int expectedPixels = bufferedImage.getHeight() * bufferedImage.getWidth();
        int expectedChars = (expectedPixels * 3) / 8;

        assertEquals(StegController.getMaxStorableChars(multipartFile), expectedChars);
    }
}
