package com.scheduler.appointment.service;

import com.scheduler.appointment.dto.CreateProviderRequest;
import com.scheduler.appointment.dto.ProviderResponse;
import com.scheduler.appointment.entity.Provider;
import com.scheduler.appointment.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Service
public class ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Transactional
    public ProviderResponse create(CreateProviderRequest request) {
        Provider provider = providerRepository.save(new Provider(request.name().trim()));
        return new ProviderResponse(provider.getId(), provider.getName());
    }

    public ResponseEntity<List<ProviderResponse>> getAllProviders() {
        return ResponseEntity.ok(providerRepository.findAll().stream()
                .map(provider -> new ProviderResponse(provider.getId(), provider.getName()))
            .toList());
    }

}
