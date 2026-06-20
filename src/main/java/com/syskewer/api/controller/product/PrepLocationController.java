package com.syskewer.api.controller.product;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syskewer.api.dto.product.PrepLocationRequestDto;
import com.syskewer.api.dto.product.PrepLocationResponseDto;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.service.product.PrepLocationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/prep-locations")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Locais de Preparo", description = "Gestão de locais de preparo (Cozinha, Churrasqueira, etc.)")
public class PrepLocationController {
    private final PrepLocationService service;
    public PrepLocationController(PrepLocationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PrepLocationResponseDto> create(@RequestBody @Valid PrepLocationRequestDto dto) {
        PrepLocation saved = service.create(dto.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new PrepLocationResponseDto(saved));
    }

    @GetMapping
    public ResponseEntity<List<PrepLocationResponseDto>> listAll() {
        List<PrepLocationResponseDto> list = service.listAll().stream()
                .map(PrepLocationResponseDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}