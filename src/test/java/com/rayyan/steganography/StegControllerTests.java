package com.rayyan.steganography;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StegControllerTests {

    @Autowired
    private MockMvc mockMvc;
    
    private static final String normalImg = "/images/normal.jpg";
    
    private static final String largeImg = "/images/15MB.jpg";
    
    private static final Path largeText = Path.of("src/test/resources/text/large_text.txt");

    private static final Path exceedsLimitText = Path.of("src/test/resources/text/exceeds_normal_limit.txt");

    private static final Path normalText = Path.of("src/test/resources/text/normal_limit.txt");

    private static final String testMsg = "Hello, this is a test message!";

    @Test
    public void testEncodeTextInImage() throws Exception {
        byte[] encoded = encodeImageWithText(normalImg, testMsg);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    @Test
    public void testDecodeTextInImage() throws Exception {
        byte[] encoded = encodeImageWithText(normalImg, testMsg);
        MockMultipartFile image = new MockMultipartFile("image", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(testMsg));
    }

    @Test
    public void testEncodeLargeTextInImage() throws Exception {
        String textToEncode = Files.readString(largeText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);

        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    @Test
    public void testDecodeLargeTextInImage() throws Exception {
        String textToEncode = Files.readString(largeText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);
        MockMultipartFile image = new MockMultipartFile("image", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(textToEncode));
    }

    @Test
    public void testEncodeLimitTextInImage() throws Exception {
        String textToEncode = Files.readString(normalText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);

        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    @Test
    public void testDecodeLimitTextInImage() throws Exception {
        String textToEncode = Files.readString(normalText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);
        MockMultipartFile image = new MockMultipartFile("image", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(textToEncode));
    }

    @Test
    public void testEncodeExceedLimitTextInImage() throws Exception {
        MockMultipartFile image = createInputFile(normalImg);
        String textToEncode = Files.readString(exceedsLimitText);
        MockMultipartFile text = new MockMultipartFile("text", "", "text/plain", textToEncode.getBytes());

        mockMvc.perform(
                        multipart("/api/encodeTI")
                                .file(image)
                                .file(text)
                ).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Text length is greater than max storable chars")));
    }

    @Test
    public void testInputVerificationSuccess() throws IOException {
        MultipartFile multipartFile = createInputFile(normalImg);
        StegController.verifyInput(multipartFile);
    }

    @Test
    public void testInputVerificationFails() throws IOException {
        MultipartFile multipartFile = createInputFile(largeImg);

        assertThrows(IOException.class, () -> {
            StegController.verifyInput(multipartFile);
        });
    }

    @Test
    public void testMaxStorableChars() throws IOException {
        MultipartFile multipartFile = createInputFile(normalImg);

        BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
        int expectedPixels = bufferedImage.getHeight() * bufferedImage.getWidth();
        int expectedChars = ((expectedPixels * 3) / 8) - 12;

        assertEquals(expectedChars, StegController.getMaxStorableChars(multipartFile), "Max storable chars did not match expected value.");
    }

    // Helpers

    private MockMultipartFile createInputFile(String input) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(input);
        assertNotNull(inputStream, "Image not found");

        String filename = Paths.get(input).getFileName().toString();
        String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";

        return new MockMultipartFile("image", filename, contentType, inputStream);
    }

    private byte[] encodeImageWithText(String imagePath, String message) throws Exception {
        MockMultipartFile image = createInputFile(imagePath);
        MockMultipartFile text = new MockMultipartFile("text", "", "text/plain", message.getBytes());

        MvcResult result = mockMvc.perform(
                multipart("/api/encodeTI")
                        .file(image)
                        .file(text)
        ).andExpect(status().isOk()).andReturn();

        return result.getResponse().getContentAsByteArray();
    }
}

