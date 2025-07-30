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
import java.util.ArrayList;
import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class StegController {

    private static ArrayList<String> mimes = new ArrayList<>(
            Arrays.asList("image/png", "image/jpeg", "image/jpg")
    );

    /**
     * Intake text and encode it into an inputted image through LSB
     *
     * @param image inputted image image
     *
     * @return original image encoded with text
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
            img = ImageIO.read(image.getInputStream());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        BufferedImage encoded = getEncodedImage(img, text);

        String contentType = image.getContentType();

        assert contentType != null;
        String formatName = contentType.split("/")[1];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(encoded, formatName, baos);
        byte[] imageBytes = baos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(imageBytes.length);
        headers.setContentDispositionFormData("attachment", "encoded." + formatName);

        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    /**
     * Intake an encoded image and output the text encoded within
     *
     * @param image inputted image
     * 
     * @return text encoded within the image
     */
    @PostMapping("/decodeTI")
    public ResponseEntity<String> decodeTextInImage(@RequestBody MultipartFile image) throws IOException {
        // Text is decoded and returned
        try {
            verifyInput(image);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("message");
    }

    private static BufferedImage getEncodedImage(BufferedImage img, String text) {
        StringBuilder bits = convertTextToBytes(text);

        int msgIdx = 0;
        int height = img.getHeight();
        int width = img.getWidth();
        BufferedImage encoded = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for(int y = 0; y < img.getHeight(); y++) {
            for(int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);

                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                if(msgIdx < bits.length()) {
                    r = (r & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                if(msgIdx < bits.length()) {
                    g = (g & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                if(msgIdx < bits.length()) {
                    b = (b & 0xFE) | (bits.charAt(msgIdx++) - '0');
                }

                int newRGB = (r << 16) | (g << 8) | b;
                encoded.setRGB(x, y, newRGB);

                if(msgIdx >= bits.length()) {
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

    private static StringBuilder convertTextToBytes(String message) {
        message += "\0\0\0\0\0\0\0\0";

        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes) {
            binary.append(String.format("%8s", Integer.toBinaryString(b * 0xFF)).replace(' ', '0'));
        }

        return binary;
    }

    /**
     * Verify input
     *
     * @param image inputted image
     *
     * @throws IOException if input is invalid
     */
    protected static void verifyInput(MultipartFile image) throws IOException {
        Tika tika = new Tika();
        if(image.getSize() > 10000000) {
            System.out.println(image.getSize());
            throw new FileSizeLimitExceededException("Image size is greater than 10 MB", image.getSize(), 10000000);
        }
        else if(!mimes.contains(tika.detect(image.getInputStream()))) {
            throw new FileUploadException("Image must be one of: " + mimes);
        }
    }

    protected static void verifyInput(MultipartFile image, String text) throws IOException {
        verifyInput(image);

        if(text.length() > getMaxStorableChars(image)) {
            throw new IOException("Text length is greater than max storable chars");
        }
    }

    protected static int getMaxStorableChars(MultipartFile image) throws IOException {
        BufferedImage file = ImageIO.read(image.getInputStream());

        if(file == null) {
            throw new IOException("Invalid image");
        }

        int pixels = file.getWidth() * file.getHeight();

        return ((pixels * 3) / 8) - 8;
    }

}
