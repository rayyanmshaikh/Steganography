package com.rayyan.steganography;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class StegController {

    @PostMapping("/encodeTI")
    public ResponseEntity<String> encodeTextInImage(@RequestBody String message) {
        return ResponseEntity.ok("message " + message);
    }

    @PostMapping("/decodeTI")
    public ResponseEntity<String> decodeTextInImage(@RequestBody String message) {
        return ResponseEntity.ok("message");
    }

}
