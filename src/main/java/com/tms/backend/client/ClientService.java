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
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.projectTemplate.ProjectTemplateRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ClientService {
    @Autowired
    private ClientRepository repo;

    @Autowired
    private NetRateSchemeRepository netRateSchemeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTemplateRepository projectTemplateRepository;

    @Transactional
    public ClientResponseDTO createClient(CreateClientRequest req) {
        if (repo.existsByNameIgnoreCase(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client name already exists");
        }

        Client client = new Client();
        client.setName(req.name());
        client.setExternalId(req.externalId());
        client.setActive(true);
        client.setEmail(req.email());
        client.setContactPerson(req.contactPerson());
        client.setContactNumber(req.contactNumber());
        client.setIndustry(req.industry());

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
        client.setEmail(req.email());
        client.setContactPerson(req.contactPerson());
        client.setContactNumber(req.contactNumber());
        client.setIndustry(req.industry());

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

    @Transactional
    public void deleteClient(Long id) {
        Client cl = repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        if (isReferenced(id)) {
            cl.setActive(false);
            repo.save(cl);
        } else {
            repo.delete(cl);
        }
    }

    private boolean isReferenced(Long clientId) {
        return projectRepository.existsByClientId(clientId)
            || projectTemplateRepository.existsByClientId(clientId);
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
            scheme != null ? scheme.getName() : null,
            client.getEmail(),
            client.getContactPerson(),
            client.getContactNumber(),
            client.getIndustry()
        );
    }
}
