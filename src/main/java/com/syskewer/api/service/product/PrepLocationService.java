package com.syskewer.api.service.product;

import java.util.List;
import org.springframework.stereotype.Service;
import com.syskewer.api.model.product.PrepLocation;
import com.syskewer.api.repository.product.PrepLocationRepository;

@Service
public class PrepLocationService {
    private final PrepLocationRepository repository;
    public PrepLocationService(PrepLocationRepository repository) { this.repository = repository; }

    public PrepLocation create(String name) {
        PrepLocation location = new PrepLocation();
        location.setName(name);
        return repository.save(location);
    }
    public List<PrepLocation> listAll() { return repository.findAll(); }
}