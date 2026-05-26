package com.tms.backend.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tms.backend.dto.ClientResponseDTO;
import com.tms.backend.dto.CreateClientRequest;
import com.tms.backend.dto.UpdateClientRequest;
import com.tms.backend.netRateScheme.NetRateScheme;
import com.tms.backend.netRateScheme.NetRateSchemeRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repo;

    @Autowired
    private NetRateSchemeRepository netRateSchemeRepository;

    public ClientResponseDTO createClient(CreateClientRequest req) {
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

    public List<ClientResponseDTO> getActiveClients() {
        return repo.findByActiveTrue().stream().map(this::toDTO).toList();
    }

    public List<ClientResponseDTO> getActiveCLOrdered() {
        return repo.findActiveClientOrderByName().stream().map(this::toDTO).toList();
    }

    public ClientResponseDTO getClientById(Long id) {
        return toDTO(repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with id: " + id)));
    }

    public ClientResponseDTO updateClient(Long id, UpdateClientRequest req) {
        Client client = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

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
