package com.smartlogix.bff.controller;

import com.smartlogix.bff.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/imagenes")
@CrossOrigin("*")
public class ImagenController {

    private final CloudinaryService service;

    public ImagenController(CloudinaryService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> subir(@RequestParam("file") MultipartFile file) {

        String url = service.subirImagen(file);

        return ResponseEntity.ok(Map.of("url", url));
    }
}