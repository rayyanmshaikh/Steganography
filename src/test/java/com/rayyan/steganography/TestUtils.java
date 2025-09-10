package com.rayyan.steganography;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {

    public static MockMultipartFile createInputFile(String input) throws IOException {
        InputStream inputStream = TestUtils.class.getResourceAsStream(input);
        assertNotNull(inputStream, "Image not found");

        String filename = Paths.get(input).getFileName().toString();
        String contentType = filename.endsWith(".png") ? "image/png" : "image/jpeg";

        return new MockMultipartFile("image", filename, contentType, inputStream);
    }
}
