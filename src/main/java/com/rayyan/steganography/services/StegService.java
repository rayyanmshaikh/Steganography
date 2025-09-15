package com.rayyan.steganography.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StegService {

    /**
     * Encodes the given text into the provided media.
     *
     * @param carrier Multipart file to encode the text into.
     * @param text  The text message to hide inside the image.
     *
     * @return A ResponseEntity containing the encoded media as a downloadable file
     */
    ResponseEntity<?> encode(MultipartFile carrier, String text) throws Exception;

    /**
     * Decodes and retrieves hidden text from the provided steganographed media.
     *
     * @param carrier Multipart image file containing hidden text.
     *
     * @return A ResponseEntity containing the decoded text
     */
    ResponseEntity<String> decode(MultipartFile carrier) throws Exception;

    /**
     * Get the type of service being used.
     *
     * @return Type of service being used
     */
    String getType();
}
