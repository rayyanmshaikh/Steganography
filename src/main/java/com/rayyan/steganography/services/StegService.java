package com.rayyan.steganography.services;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface StegService {
    ResponseEntity<?> encode(MultipartFile carrier, String text) throws Exception;

    ResponseEntity<String> decode(MultipartFile carrier) throws Exception;

    String getType();
}
