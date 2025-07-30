package com.rayyan.steganography;

import org.apache.tika.Tika;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class StegController {

    @Value("steg.mime.allowed")
    private static ArrayList<String> mimes;

    /**
     * Intake text and encode it into an inputted image through LSB
     *
     * @param image inputted image image
     *
     * @return original image encoded with text
     */
    @PostMapping("/encodeTI")
    public ResponseEntity<String> encodeTextInImage(@RequestBody MultipartFile image, @RequestParam("text") String text) {
        // Encode within
        // Return new image

        try {
            verifyInput(image, text);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("message");
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

    /**
     * Verify input
     *
     * @param image inputted image
     *
     * @throws IOException if input is invalid
     */
    protected static void verifyInput(MultipartFile image) throws IOException {
        Tika tika = new Tika();
        if(image.getSize() > 100000) {
            throw new FileSizeLimitExceededException("Image size is greater than 10 MB", 100000, image.getSize());
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

        return (pixels * 3) / 8;
    }

}
