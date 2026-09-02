package com.tms.backend.netRateScheme;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tms.backend.client.Client;
import com.tms.backend.client.ClientRepository;
import com.tms.backend.dto.MatchTypeRateDTO;
import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeCreateDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.NetRateSchemeUpdateDTO;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.quote.QuoteRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class NetRateSchemeService {
    private static final Logger log = LoggerFactory.getLogger(NetRateSchemeService.class);

    private final NetRateSchemeRepository netRateSchemeRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final QuoteRepository quoteRepository;

    public NetRateSchemeService(
        NetRateSchemeRepository netRateSchemeRepository,
        UserRepository userRepository,
        ClientRepository clientRepository,
        ProjectRepository projectRepository,
        QuoteRepository quoteRepository
    ) {
        this.netRateSchemeRepository = netRateSchemeRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.quoteRepository = quoteRepository;
    }

    @Transactional
    public NetRateScheme createScheme(NetRateSchemeCreateDTO dto, Long userId) {
        NetRateScheme scheme = new NetRateScheme();
        scheme.setName(dto.name());

        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        scheme.setCreatedBy(creator);

        if (dto.isDefault()) {
            netRateSchemeRepository.clearCurrentDefault();
            scheme.setDefault(true);
        } else if (!netRateSchemeRepository.existsByIsDefaultTrue()) {
            scheme.setDefault(true);
        }

        if (dto.matchTypeRates() != null) {
            for (MatchTypeRateDTO rateDto : dto.matchTypeRates()) {
                MatchTypeRate rate = toMatchTypeRate(rateDto);
                rate.setNetRateScheme(scheme);
                scheme.getMatchTypeRates().add(rate);
            }
        }

        NetRateScheme savedScheme = netRateSchemeRepository.save(scheme);

        if (dto.clientId() != null) {
            Client client = clientRepository.findById(dto.clientId())
                    .orElseThrow(() -> new RuntimeException("Client not found"));
            client.setNetRateScheme(savedScheme);
            clientRepository.save(client);
        }

        return savedScheme;
    }

    public List<NetRateSchemeResponseDTO> getAllSchemes() {
        return netRateSchemeRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public NetRateSchemeResponseDTO getSchemeById(Long id) {
        NetRateScheme scheme = netRateSchemeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));
        return toDTO(scheme);
    }

    public NetRateSchemeResponseDTO toDTO(NetRateScheme scheme) {
        List<MatchTypeRateResponseDTO> rates = scheme.getMatchTypeRates().stream()
                .map(m -> new MatchTypeRateResponseDTO(
                        m.getMatchType(),
                        m.getTransMemoryPercent()
                        // m.getMachineTransPercent(),
                        // m.getNonTranslatablePercent(),
                        // m.getInternalFuzziesPercent()
                ))
                .toList();

        Client client = clientRepository.findByNetRateScheme(scheme).orElse(null);

        return new NetRateSchemeResponseDTO(
                scheme.getId(),
                scheme.getName(),
                scheme.isDefault(),
                rates,
                client != null ? client.getId() : null,
                client != null ? client.getName() : null
        );
    }

    public NetRateSchemeResponseDTO getDefaultScheme() {
        NetRateScheme scheme = netRateSchemeRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new RuntimeException("No default NetRateScheme set"));
        return toDTO(scheme);
    }

    @Transactional
    public NetRateSchemeResponseDTO getSchemeByClientId(long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        NetRateScheme scheme = client.getNetRateScheme();
        if (scheme == null) {
            scheme = netRateSchemeRepository.findByIsDefaultTrue()
                    .orElseThrow(() -> new RuntimeException("No default NetRateScheme set"));
        }
        return toDTO(scheme);
    }

    @Transactional
    public NetRateScheme updateScheme(Long schemeId, NetRateSchemeUpdateDTO dto) {
        NetRateScheme scheme = netRateSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        if (dto.name() != null) {
            scheme.setName(dto.name());
        }

        scheme.getMatchTypeRates().clear();

        if (dto.matchTypeRates() != null) {
            for (MatchTypeRateDTO rateDto : dto.matchTypeRates()) {
                MatchTypeRate rate = toMatchTypeRate(rateDto);
                rate.setNetRateScheme(scheme);
                scheme.getMatchTypeRates().add(rate);
            }
        }

        return netRateSchemeRepository.save(scheme);
    }

    @Transactional
    public void setDefault(Long schemeId) {
        NetRateScheme scheme = netRateSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        netRateSchemeRepository.clearCurrentDefault();
        scheme.setDefault(true);
        netRateSchemeRepository.save(scheme);
    }

    @Transactional
    public void deleteSchemes(List<Long> ids) {
        List<NetRateScheme> schemes = netRateSchemeRepository.findAllById(ids);

        if (schemes.isEmpty()) {
            throw new RuntimeException("No schemes found for the given IDs");
        }

        projectRepository.clearNetRateSchemeByIds(ids);
        clientRepository.clearNetRateSchemeByIds(ids);
        quoteRepository.clearNetRateSchemeByIds(ids);
        netRateSchemeRepository.deleteAll(schemes);
    }

    @Transactional
    public NetRateScheme duplicateScheme(Long sourceSchemeId, Long userId) {
        NetRateScheme original = netRateSchemeRepository.findById(sourceSchemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        NetRateScheme copy = new NetRateScheme();
        copy.setName(original.getName() + " (Copy)");
        copy.setCreatedBy(creator);

        for (MatchTypeRate original_rate : original.getMatchTypeRates()) {
            MatchTypeRate rateCopy = new MatchTypeRate(
                original_rate.getMatchType(),
                original_rate.getTransMemoryPercent()
                // original_rate.getMachineTransPercent(),
                // original_rate.getNonTranslatablePercent(),
                // original_rate.getInternalFuzziesPercent()
            );
            rateCopy.setNetRateScheme(copy);
            copy.getMatchTypeRates().add(rateCopy);
        }

        return netRateSchemeRepository.save(copy);
    }

    @Transactional
    public void insertDefaultNetRateSchemeIfMissing() {
        if (netRateSchemeRepository.existsByIsDefaultTrue()) return;

        NetRateScheme scheme = new NetRateScheme();
        scheme.setName("Default");
        scheme.setDefault(true);

        for (MatchTypeRate rate : createDefaultMatchTypeRates()) {
            rate.setNetRateScheme(scheme);
            scheme.getMatchTypeRates().add(rate);
        }

        netRateSchemeRepository.save(scheme);
    }

    private List<MatchTypeRate> createDefaultMatchTypeRates() {
        return List.of(
            new MatchTypeRate(MatchType.REPETITIONS, 30L /*, 0L, 0L, 0L*/),
            new MatchTypeRate(MatchType.PERCENT_101, 0L /*, 0L, 0L, 0L*/),
            new MatchTypeRate(MatchType.PERCENT_100, 0L /*, 30L, 10L, 10L*/),
            new MatchTypeRate(MatchType.PERCENT_95, 70L /*, 40L, 33L, 33L*/),
            new MatchTypeRate(MatchType.PERCENT_85, 70L /*, 70L, 66L, 66L*/),
            new MatchTypeRate(MatchType.PERCENT_75, 100L /*, 100L, 100L, 100L*/),
            new MatchTypeRate(MatchType.PERCENT_50, 100L /*, 100L, 100L, 100L*/),
            new MatchTypeRate(MatchType.PERCENT_0, 100L /*, 100L, 100L, 100L*/)
        );
    }

    /**
     * Resolves which NetRateScheme to use for sizing a project's files: the project's own
     * scheme, falling back to its client's scheme, falling back to the global default —
     * skipping any scheme along the way that has no match type rates configured.
     */
    public NetRateSchemeResponseDTO resolveSchemeForProject(Project project) {
        NetRateScheme projectScheme = project.getNetRateScheme();
        if (projectScheme != null) {
            NetRateSchemeResponseDTO dto = toDTO(projectScheme);
            if (!dto.matchTypeRates().isEmpty()) return dto;
            log.warn("Project scheme '{}' (id={}) has no match type rates, falling back to default", dto.name(), dto.id());
        }
        if (project.getClient() != null) {
            NetRateScheme clientScheme = project.getClient().getNetRateScheme();
            if (clientScheme != null) {
                NetRateSchemeResponseDTO dto = toDTO(clientScheme);
                if (!dto.matchTypeRates().isEmpty()) return dto;
                log.warn("Client scheme '{}' (id={}) has no match type rates, falling back to default", dto.name(), dto.id());
            }
        }
        return getDefaultScheme();
    }

    /**
     * Builds the sizingRequestJson field sent to Tomato's sizing APIs: the resolved
     * NetRateScheme (match-type rates + NT rules) plus the projectId, as a JSON string.
     */
    public String buildSizingRequestJson(NetRateSchemeResponseDTO scheme, Long projectId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("id", scheme.id());
            root.put("name", scheme.name());
            root.put("projectId", projectId);

            ObjectNode ntRules = root.putObject("ntRules");
            ntRules.putArray("regexPatterns");
            ntRules.putArray("staticTerms");
            ntRules.putArray("exactTerms");
            ntRules.putArray("inlineElements");

            ArrayNode matchTypeRates = root.putArray("matchTypeRates");
            for (MatchTypeRateResponseDTO rate : scheme.matchTypeRates()) {
                ObjectNode rateNode = matchTypeRates.addObject();
                rateNode.put("matchType", rate.matchType().name());
                rateNode.put("transMemoryPercent", rate.transMemoryPercent() != null ? rate.transMemoryPercent() : 0L);
            }
            String json = mapper.writeValueAsString(root);
            String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            log.info("sizingRequestJson sent to Tomato:\n{}", prettyJson);
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build sizingRequestJson", e);
        }
    }

    private MatchTypeRate toMatchTypeRate(MatchTypeRateDTO dto) {
        MatchTypeRate rate = new MatchTypeRate();
        rate.setMatchType(dto.matchType());
        rate.setTransMemoryPercent(dto.transMemoryPercent());
        // rate.setMachineTransPercent(dto.machineTransPercent());
        // rate.setNonTranslatablePercent(dto.nonTranslatablePercent());
        // rate.setInternalFuzziesPercent(dto.internalFuzziesPercent());
        return rate;
    }
}
