package com.rayyan.steganography;

import com.rayyan.steganography.services.StegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for handling image steganography operations.
 * Provides endpoints for encoding text into an image and decoding text from an image.
 */
@RestController
@RequestMapping("/api")
public class StegController {
    private static final Logger logger = LoggerFactory.getLogger(StegController.class);

    private final Map<String, StegService> services;

    /**
     * Initialize the controller
     * @param serviceList List of available services injected through Spring Boot Beans
     */
    public StegController(List<StegService> serviceList) {
        this.services = serviceList.stream().collect(Collectors.toMap(StegService::getType, s -> s));
    }

    /**
     * Encodes the given text into the provided media.
     *
     * @param type Type of service to use
     * @param carrier Multipart file to encode the text into.
     * @param payload  The text message to hide inside the image.
     *
     * @return A ResponseEntity containing the encoded media as a downloadable file
     */
    @PostMapping(value = "/{type}/encodeTI", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> encode(
            @PathVariable String type,
            @RequestPart("carrier") MultipartFile carrier,
            @RequestPart("text") String payload) {
        try {
            logger.info("Recieved encoding request of type: {}", type);
            StegService service = services.get(type);

            if (service == null) return ResponseEntity.badRequest().body("Unknown type: " + type);

            return service.encode(carrier, payload);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Decodes and retrieves hidden text from the provided steganographed media.
     *
     * @param type Type of service to use
     * @param carrier Multipart image file containing hidden text.
     *
     * @return A ResponseEntity containing the decoded text
     */
    @PostMapping(value = "/{type}/decodeTI", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> decodeTextInImage(
            @PathVariable String type,
            @RequestPart("carrier") MultipartFile carrier) {
        try {
            logger.info("Recieved decoding request of type: {}", type);
            StegService service = services.get(type);

            if (service == null) return ResponseEntity.badRequest().body("Unknown type: " + type);

            return service.decode(carrier);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
