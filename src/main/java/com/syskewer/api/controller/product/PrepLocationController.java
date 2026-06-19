package com.syskewer.api.controller.product;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.syskewer.api.dto.product.PrepLocationRequestDto;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.service.product.PrepLocationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/prep-locations")
public class PrepLocationController {
    private final PrepLocationService service;
    public PrepLocationController(PrepLocationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PrepLocation> create(@RequestBody @Valid PrepLocationRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto.name()));
    }

    @GetMapping
    public ResponseEntity<List<PrepLocation>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }
}