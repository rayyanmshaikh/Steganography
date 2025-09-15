package com.rayyan.steganography;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.rayyan.steganography.TestUtils.createInputFile;
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

    private static final Path largeText = Path.of("src/test/resources/text/large_text.txt");

    private static final Path exceedsLimitText = Path.of("src/test/resources/text/exceeds_normal_limit.txt");

    private static final Path normalText = Path.of("src/test/resources/text/normal_limit.txt");

    private static final String testMsg = "Hello, this is a test message!";

    /**
     * Tests encoding a simple text message into an image and verifies the result is
     * not null or empty.
     *
     * @throws Exception if encoding fails
     */
    @Test
    public void testEncodeTextInImage() throws Exception {
        byte[] encoded = encodeImageWithText(normalImg, testMsg);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests decoding a simple text message from an image and verifies the decoded
     * message matches the original.
     *
     * @throws Exception if decoding fails
     */
    @Test
    public void testDecodeTextInImage() throws Exception {
        byte[] encoded = encodeImageWithText(normalImg, testMsg);
        MockMultipartFile image = new MockMultipartFile("carrier", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/text-in-image/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(testMsg));
    }

    /**
     * Tests encoding a large text message into an image and verifies the result is
     * not null or empty.
     *
     * @throws Exception if encoding fails
     */
    @Test
    public void testEncodeLargeTextInImage() throws Exception {
        String textToEncode = Files.readString(largeText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);

        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests decoding a large text message from an image and verifies the decoded
     * message matches the original.
     *
     * @throws Exception if decoding fails
     */
    @Test
    public void testDecodeLargeTextInImage() throws Exception {
        String textToEncode = Files.readString(largeText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);
        MockMultipartFile image = new MockMultipartFile("carrier", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/text-in-image/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(textToEncode));
    }

    /**
     * Tests encoding a text message that is at the storage limit for the image and
     * verifies the result is not null or empty.
     *
     * @throws Exception if encoding fails
     */
    @Test
    public void testEncodeLimitTextInImage() throws Exception {
        String textToEncode = Files.readString(normalText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);

        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests decoding a text message that is at the storage limit for the image and
     * verifies the decoded message matches the original.
     *
     * @throws Exception if decoding fails
     */
    @Test
    public void testDecodeLimitTextInImage() throws Exception {
        String textToEncode = Files.readString(normalText);
        byte[] encoded = encodeImageWithText(normalImg, textToEncode);
        MockMultipartFile image = new MockMultipartFile("carrier", "encoded.png", "image/png", encoded);

        mockMvc.perform(multipart("/api/text-in-image/decodeTI").file(image))
                .andExpect(status().isOk())
                .andExpect(content().string(textToEncode));
    }

    /**
     * Tests encoding a text message that exceeds the storage limit for the image
     * and expects a bad request response.
     *
     * @throws Exception if encoding fails
     */
    @Test
    public void testEncodeExceedLimitTextInImage() throws Exception {
        MockMultipartFile image = createInputFile(normalImg);
        String textToEncode = Files.readString(exceedsLimitText);
        MockMultipartFile text = new MockMultipartFile("text", "", "text/plain", textToEncode.getBytes());

        mockMvc.perform(
                multipart("/api/text-in-image/encodeTI")
                        .file(image)
                        .file(text))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Text length is greater than max storable chars")));
    }

    // Helpers

    /**
     * Helper method to encode a text message into an image using the controller's
     * API.
     *
     * @param imagePath the path to the image file
     * @param message   the text message to encode
     * @return the encoded image as a byte array
     * @throws Exception if encoding fails
     */
    private byte[] encodeImageWithText(String imagePath, String message) throws Exception {
        MockMultipartFile image = createInputFile(imagePath);
        MockMultipartFile text = new MockMultipartFile("text", "", "text/plain", message.getBytes());

        MvcResult result = mockMvc.perform(
                multipart("/api/text-in-image/encodeTI")
                        .file(image)
                        .file(text))
                .andExpect(status().isOk()).andReturn();

        return result.getResponse().getContentAsByteArray();
    }
}
