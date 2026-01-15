package com.tms.backend.netRateScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeCreateDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.NetRateSchemeUpdateDTO;
import com.tms.backend.dto.NetRateSchemeWfDTO;
import com.tms.backend.dto.WorkflowStepRateResponseDTO;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;
import com.tms.backend.workflowSteps.WorkflowStep;
import com.tms.backend.workflowSteps.WorkflowStepRepository;

import jakarta.transaction.Transactional;

@Service
public class NetRateSchemeService {
    private final NetRateSchemeRepository netRateSchemeRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowStepRepository workflowStepRepo;

    public NetRateSchemeService(
        NetRateSchemeRepository netRateSchemeRepository,
        UserRepository userRepository,
        ProjectRepository projectRepository,
        WorkflowStepRepository workflowStepRepo
    ) 
    {
        this.netRateSchemeRepository = netRateSchemeRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.workflowStepRepo = workflowStepRepo;
    }

    @Transactional
    public NetRateScheme createScheme(NetRateSchemeCreateDTO dto, Long userId) {
        NetRateScheme scheme = new NetRateScheme();
        scheme.setName(dto.name());
        // set creator
        User creator = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        scheme.setCreatedBy(creator);

        // set project
        Project project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        scheme.setProject(project);


        if (dto.workflowSteps() != null) {
            for (NetRateSchemeWfDTO wfDto : dto.workflowSteps()) {

                WorkflowStep workflowStep = workflowStepRepo
                    .findById(wfDto.workflowStepId())
                    .orElseThrow(() -> new RuntimeException(
                            "WorkflowStep not found: " + wfDto.workflowStepId()
                    ));

                WorkflowStepRate wfRate = new WorkflowStepRate();
                wfRate.setWorkflowStep(workflowStep); // entity reference
                wfRate.setNetRateScheme(scheme); // back-reference

                List<MatchTypeRate> rates = wfDto.matchTypeRates().stream()
                        .map(m -> {
                            MatchTypeRate rate = new MatchTypeRate();
                            rate.setMatchType(m.matchType());
                            rate.setTransMemoryPercent(m.transMemoryPercent());
                            rate.setMachineTransPercent(m.machineTransPercent());
                            rate.setNonTranslatablePercent(m.nonTranslatablePercent());
                            rate.setInternalFuzziesPercent(m.internalFuzziesPercent());
                            return rate;
                        })
                        .toList();

                wfRate.setMatchTypeRates(rates);
                scheme.getWorkflowStepRates().add(wfRate);
            }
        }

        return netRateSchemeRepository.save(scheme);
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

    private NetRateSchemeResponseDTO toDTO(NetRateScheme scheme) {
        List<WorkflowStepRateResponseDTO> wfDtos = scheme.getWorkflowStepRates().stream()
                .map(wf -> new WorkflowStepRateResponseDTO(
                        wf.getWorkflowStep().getId(),
                        wf.getMatchTypeRates().stream()
                                .map(m -> new MatchTypeRateResponseDTO(
                                        m.getMatchType(),
                                        m.getTransMemoryPercent(),
                                        m.getMachineTransPercent(),
                                        m.getNonTranslatablePercent(),
                                        m.getInternalFuzziesPercent()
                                ))
                                .toList()
                ))
                .toList();

        return new NetRateSchemeResponseDTO(
                scheme.getId(),
                scheme.getName(),
                scheme.getProject().getId(),
                wfDtos
        );
    }

    @Transactional
    public NetRateScheme updateScheme(Long schemeId, NetRateSchemeUpdateDTO dto) {
        // fetch existing scheme
        NetRateScheme scheme = netRateSchemeRepository.findById(schemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        // update fields
        if (dto.name() != null) {
            scheme.setName(dto.name());
        }

        // Clear existing workflow steps (orphanRemoval will delete child records)
        scheme.getWorkflowStepRates().clear();

        // 4️⃣ Add new workflow steps from DTO
        if (dto.netRateSchemeWfList() != null) {
            for (NetRateSchemeWfDTO wfDto : dto.netRateSchemeWfList()) {
                WorkflowStep workflowStep = workflowStepRepo
                    .findById(wfDto.workflowStepId())
                    .orElseThrow(() -> new RuntimeException(
                            "WorkflowStep not found: " + wfDto.workflowStepId()
                    ));

                WorkflowStepRate wf = new WorkflowStepRate();
                wf.setWorkflowStep(workflowStep);
                wf.setNetRateScheme(scheme); // back-reference

                List<MatchTypeRate> rates = wfDto.matchTypeRates().stream()
                        .map(m -> {
                            MatchTypeRate rate = new MatchTypeRate();
                            rate.setMatchType(m.matchType());
                            rate.setTransMemoryPercent(m.transMemoryPercent());
                            rate.setMachineTransPercent(m.machineTransPercent());
                            rate.setNonTranslatablePercent(m.nonTranslatablePercent());
                            rate.setInternalFuzziesPercent(m.internalFuzziesPercent());
                            return rate;
                        })
                        .toList();

                wf.setMatchTypeRates(rates);
                scheme.getWorkflowStepRates().add(wf);
            }
        }

        // save the scheme (cascade will persist workflow steps and match type rates)
        return netRateSchemeRepository.save(scheme);
    }

    public void deleteSchemes(List<Long> ids) {
        List<NetRateScheme> schemes = netRateSchemeRepository.findAllById(ids);

        if (schemes.isEmpty()) {
            throw new RuntimeException("No schemes found for the given IDs");
        }

        netRateSchemeRepository.deleteAll(schemes);
    }

    @Transactional
    public NetRateScheme duplicateScheme(Long sourceSchemeId, Long userId) {
        // fetch source scheme
        NetRateScheme original = netRateSchemeRepository.findById(sourceSchemeId)
                .orElseThrow(() -> new RuntimeException("NetRateScheme not found"));

        // fetch creator
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // create new scheme
        NetRateScheme copy = new NetRateScheme();
        copy.setName(original.getName() + " (Copy)");
        copy.setProject(original.getProject());
        copy.setCreatedBy(creator);

        for (WorkflowStepRate originalWf : original.getWorkflowStepRates()) {

            WorkflowStepRate wfCopy = new WorkflowStepRate();
            wfCopy.setWorkflowStep(originalWf.getWorkflowStep());
            wfCopy.setNetRateScheme(copy); // important: back-reference

            // duplicate match type rates
            List<MatchTypeRate> rateCopies = originalWf.getMatchTypeRates().stream()
                    .map(rate -> {
                        MatchTypeRate r = new MatchTypeRate();
                        r.setMatchType(rate.getMatchType());
                        r.setTransMemoryPercent(rate.getTransMemoryPercent());
                        r.setMachineTransPercent(rate.getMachineTransPercent());
                        r.setNonTranslatablePercent(rate.getNonTranslatablePercent());
                        r.setInternalFuzziesPercent(rate.getInternalFuzziesPercent());
                        return r;
                    })
                    .toList();

            wfCopy.setMatchTypeRates(rateCopies);
            copy.getWorkflowStepRates().add(wfCopy);
        }

        // save
        return netRateSchemeRepository.save(copy);
    }

    @Transactional
    public void addStepToAllSchemes(Long workflowStepId) {
        // define default percentages for each match type
        Map<MatchType, MatchTypeRate> defaultMatchTypeRates = Map.of(
            MatchType.REPETITIONS, new MatchTypeRate(MatchType.REPETITIONS, 10L, 0L, 0L, 0L),
            MatchType.PERCENT_101, new MatchTypeRate(MatchType.PERCENT_101, 10L, 0L, 0L, 0L),
            MatchType.PERCENT_100, new MatchTypeRate(MatchType.PERCENT_100, 10L, 30L, 10L, 10L),
            MatchType.PERCENT_95_99, new MatchTypeRate(MatchType.PERCENT_95_99, 33L, 40L, 33L, 33L),
            MatchType.PERCENT_85_94, new MatchTypeRate(MatchType.PERCENT_85_94, 66L, 70L, 66L, 66L),
            MatchType.PERCENT_75_84, new MatchTypeRate(MatchType.PERCENT_75_84, 100L, 100L, 100L, 100L),
            MatchType.PERCENT_50_74, new MatchTypeRate(MatchType.PERCENT_50_74, 100L, 100L, 100L, 100L),
            MatchType.PERCENT_0_49, new MatchTypeRate(MatchType.PERCENT_0_49, 100L, 100L, 100L, 100L)
        );

        // fetch workflow step once
        WorkflowStep workflowStep = workflowStepRepo.findById(workflowStepId)
                .orElseThrow(() -> new RuntimeException(
                        "WorkflowStep not found: " + workflowStepId));

        // fetch all schemes
        List<NetRateScheme> schemes = netRateSchemeRepository.findAll();

        // track only modified schemes
        List<NetRateScheme> modifiedSchemes = new ArrayList<>();

        for (NetRateScheme scheme : schemes) {

            boolean exists = scheme.getWorkflowStepRates().stream()
                    .anyMatch(ws -> ws.getWorkflowStep().getId().equals(workflowStepId));

            if (!exists) {
                WorkflowStepRate newStep = new WorkflowStepRate();
                newStep.setWorkflowStep(workflowStep);
                newStep.setNetRateScheme(scheme);

                for (MatchTypeRate mt : defaultMatchTypeRates.values()) {
                    MatchTypeRate copy = new MatchTypeRate();
                    copy.setMatchType(mt.getMatchType());
                    copy.setTransMemoryPercent(mt.getTransMemoryPercent());
                    copy.setMachineTransPercent(mt.getMachineTransPercent());
                    copy.setNonTranslatablePercent(mt.getNonTranslatablePercent());
                    copy.setInternalFuzziesPercent(mt.getInternalFuzziesPercent());
                    newStep.getMatchTypeRates().add(copy);
                }

                scheme.getWorkflowStepRates().add(newStep);
                modifiedSchemes.add(scheme); // track change
            }
        }

        // save only modified schemes
        if (!modifiedSchemes.isEmpty()) {
            netRateSchemeRepository.saveAll(modifiedSchemes);
        }
    }
}