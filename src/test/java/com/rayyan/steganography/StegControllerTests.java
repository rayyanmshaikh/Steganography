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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StegControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testEncodeTextInImage() throws Exception {
        cleanUpOutputFile("src/test/resources/images_encoded/encoded_normal.png");
        MockMultipartFile image = createInputFile("/images/normal.jpg");

        String textToEncode = "Hello, this is a test message!";
        MockMultipartFile text = new MockMultipartFile(
                "text",
                "",
                "text/plain",
                textToEncode.getBytes()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/api/encodeTI")
                                .file(image)
                                .file(text)
                ).andExpect(status().isOk())
                .andReturn();

        byte[] encodedImageBytes = result.getResponse().getContentAsByteArray();

        Path outputPath = Paths.get("src/test/resources/images_encoded/encoded_normal.png");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, encodedImageBytes);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    public void testDecodeTextInImage() throws Exception {
        Path encodedPath = Paths.get("src/test/resources/images_encoded/encoded_normal.png");
        assumeTrue(Files.exists(encodedPath), "encoded_normal.png must exist");

        MockMultipartFile image = createInputFile("/images_encoded/encoded_normal.png");
        String expected = "Hello, this is a test message!";

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(expected));
    }

    @Test
    public void testEncodeLargeTextInImage() throws Exception {
        cleanUpOutputFile("src/test/resources/images_encoded/encoded_normal.png");
        MockMultipartFile image = createInputFile("/images/normal.jpg");

        String textToEncode = Files.readString(Path.of("src/test/resources/text/large_text.txt"));
        MockMultipartFile text = new MockMultipartFile(
                "text",
                "",
                "text/plain",
                textToEncode.getBytes()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/api/encodeTI")
                                .file(image)
                                .file(text)
                ).andExpect(status().isOk())
                .andReturn();

        byte[] encodedImageBytes = result.getResponse().getContentAsByteArray();

        Path outputPath = Paths.get("src/test/resources/images_encoded/encoded_large_normal.png");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, encodedImageBytes);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    public void testDecodeLargeTextInImage() throws Exception {
        Path encodedPath = Paths.get("src/test/resources/images_encoded/encoded_large_normal.png");
        assumeTrue(Files.exists(encodedPath), "encoded_large_normal.png must exist");

        MockMultipartFile image = createInputFile("/images_encoded/encoded_large_normal.png");
        String expected = Files.readString(Path.of("src/test/resources/text/large_text.txt"));

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(expected));
    }

    @Test
    public void testEncodeLimitTextInImage() throws Exception {
        cleanUpOutputFile("src/test/resources/images_encoded/encoded_normal.png");
        MockMultipartFile image = createInputFile("/images/normal.jpg");

        String textToEncode = Files.readString(Path.of("src/test/resources/text/normal_limit.txt"));
        MockMultipartFile text = new MockMultipartFile(
                "text",
                "",
                "text/plain",
                textToEncode.getBytes()
        );

        MvcResult result = mockMvc.perform(
                        multipart("/api/encodeTI")
                                .file(image)
                                .file(text)
                ).andExpect(status().isOk())
                .andReturn();

        byte[] encodedImageBytes = result.getResponse().getContentAsByteArray();

        Path outputPath = Paths.get("src/test/resources/images_encoded/encoded_normal_limit.png");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, encodedImageBytes);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    public void testDecodeLimitTextInImage() throws Exception {
        Path encodedPath = Paths.get("src/test/resources/images_encoded/encoded_normal_limit.png");
        assumeTrue(Files.exists(encodedPath), "encoded_normal_limit.png must exist");

        MockMultipartFile image = createInputFile("/images_encoded/encoded_normal_limit.png");
        String expected = Files.readString(Path.of("src/test/resources/text/normal_limit.txt"));

        mockMvc.perform(multipart("/api/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(expected));
    }

    @Test
    public void testEncodeExceedLimitTextInImage() throws Exception {
        cleanUpOutputFile("src/test/resources/images_encoded/encoded_normal.png");
        MockMultipartFile image = createInputFile("/images/normal.jpg");

        String textToEncode = Files.readString(Path.of("src/test/resources/text/exceeds_normal_limit.txt"));
        MockMultipartFile text = new MockMultipartFile(
                "text",
                "",
                "text/plain",
                textToEncode.getBytes()
        );

        mockMvc.perform(
                        multipart("/api/encodeTI")
                                .file(image)
                                .file(text)
                ).andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Text length is greater than max storable chars")));
    }

    @Test
    public void testInputVerificationSuccess() throws IOException {
        MultipartFile multipartFile = createInputFile("/images/normal.jpg");

        StegController.verifyInput(multipartFile);
    }

    @Test
    public void testInputVerificationFails() throws IOException {
        MultipartFile multipartFile = createInputFile("/images/15MB.jpg");

        assertThrows(IOException.class, () -> {
            StegController.verifyInput(multipartFile);
        });
    }

    @Test
    public void testMaxStorableChars() throws IOException {
        MultipartFile multipartFile = createInputFile("/images/normal.jpg");

        BufferedImage bufferedImage = ImageIO.read(multipartFile.getInputStream());
        int expectedPixels = bufferedImage.getHeight() * bufferedImage.getWidth();
        int expectedChars = ((expectedPixels * 3) / 8) - 8;

        assertEquals(expectedChars, StegController.getMaxStorableChars(multipartFile), "Max storable chars did not match expected value.");
    }

    private MockMultipartFile createInputFile(String input) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(input);
        assertNotNull(inputStream, "Image not found");

        String filename = Paths.get(input).getFileName().toString();
        String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";

        return new MockMultipartFile(
                "image",
                input.split("/")[2],
                contentType,
                inputStream
        );
    }

    void cleanUpOutputFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                System.err.println("Warning: Could not delete existing output file.");
            }
        }
    }
}
