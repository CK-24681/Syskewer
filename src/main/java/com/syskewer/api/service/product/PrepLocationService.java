package com.syskewer.api.service.product;

import java.util.List;
import org.springframework.stereotype.Service;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.repository.product.PrepLocationRepository;

// Servico para gerenciar os locais de preparo dos produtos (ex: Cozinha, Churrasqueira)
@Service
public class PrepLocationService {
    private final PrepLocationRepository repository;
    public PrepLocationService(PrepLocationRepository repository) { this.repository = repository; }

    // Cadastra um novo local de preparo
    public PrepLocation create(String name) {
        PrepLocation location = new PrepLocation();
        location.setName(name);
        return repository.save(location);
    }
    // Lista todos os locais de preparo cadastrados
    public List<PrepLocation> listAll() { return repository.findAll(); }
}