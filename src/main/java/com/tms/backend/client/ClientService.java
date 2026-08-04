package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tms.backend.dto.ClientResponseDTO;
import com.tms.backend.dto.CreateClientRequest;
import com.tms.backend.dto.UpdateClientRequest;
import com.tms.backend.netRateScheme.NetRateScheme;
import com.tms.backend.netRateScheme.NetRateSchemeRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repo;

    @Autowired
    private NetRateSchemeRepository netRateSchemeRepository;

    @Transactional
    public ClientResponseDTO createClient(CreateClientRequest req) {
        if (repo.existsByNameIgnoreCase(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client name already exists");
        }

        Client client = new Client();
        client.setName(req.name());
        client.setExternalId(req.externalId());
        client.setActive(true);

        if (req.netRateSchemeId() != null) {
            NetRateScheme scheme = netRateSchemeRepository.findById(req.netRateSchemeId())
                .orElseThrow(() -> new EntityNotFoundException("NetRateScheme not found"));
            client.setNetRateScheme(scheme);
        }

        return toDTO(repo.save(client));
    }

    @Transactional
    public List<ClientResponseDTO> getActiveClients() {
        return repo.findByActiveTrue().stream().map(this::toDTO).toList();
    }

    @Transactional
    public List<ClientResponseDTO> getActiveCLOrdered() {
        return repo.findActiveClientOrderByName().stream().map(this::toDTO).toList();
    }

    @Transactional
    public ClientResponseDTO getClientById(Long id) {
        return toDTO(repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id)));
    }

    @Transactional
    public ClientResponseDTO updateClient(Long id, UpdateClientRequest req) {
        Client client = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        if (repo.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client name already exists");
        }

        client.setName(req.name());
        client.setExternalId(req.externalId());

        if (req.netRateSchemeId() != null) {
            NetRateScheme scheme = netRateSchemeRepository.findById(req.netRateSchemeId())
                .orElseThrow(() -> new EntityNotFoundException("NetRateScheme not found"));
            client.setNetRateScheme(scheme);
        } else {
            client.setNetRateScheme(null);
        }

        return toDTO(repo.save(client));
    }

    @Transactional
    public boolean nameExists(String name, Long excludeId) {
        return excludeId == null
            ? repo.existsByNameIgnoreCase(name)
            : repo.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    public void softDeleteClient(Long id) {
        Client cl = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));
        cl.setActive(false);
        repo.save(cl);
    }

    public void hardDeleteClient(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Client not found");
        }
        repo.deleteById(id);
    }

    private ClientResponseDTO toDTO(Client client) {
        NetRateScheme scheme = client.getNetRateScheme();
        return new ClientResponseDTO(
            client.getId(),
            client.getUuid(),
            client.getName(),
            client.getExternalId(),
            client.isActive(),
            scheme != null ? scheme.getId() : null,
            scheme != null ? scheme.getName() : null
        );
    }
}
