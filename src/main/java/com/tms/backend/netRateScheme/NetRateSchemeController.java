package com.tms.backend.netRateScheme;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.MatchTypeRateResponseDTO;
import com.tms.backend.dto.NetRateSchemeCreateDTO;
import com.tms.backend.dto.NetRateSchemeDeleteRequestDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.NetRateSchemeUpdateDTO;
import com.tms.backend.dto.WorkflowStepRateResponseDTO;
import com.tms.backend.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/net-rate-scheme")
@RequiredArgsConstructor
public class NetRateSchemeController {
    private final NetRateSchemeService netRateSchemeService;

    @PostMapping("/create")
    public ResponseEntity<NetRateScheme> createScheme(
                @RequestBody NetRateSchemeCreateDTO dto,
                Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        NetRateScheme savedScheme = netRateSchemeService.createScheme(dto, userDetails.getId());
        // return ResponseEntity.status(HttpStatus.CREATED).build();
        return ResponseEntity.ok(savedScheme);
    }

    // fetch all schemes
    @GetMapping("/all")
    public ResponseEntity<List<NetRateSchemeResponseDTO>> getAllSchemes() {
        List<NetRateSchemeResponseDTO> dtos = netRateSchemeService.getAllSchemes();
        return ResponseEntity.ok(dtos);
    }

    // fetch scheme by ID
    @GetMapping("/{id}")
    public ResponseEntity<NetRateSchemeResponseDTO> getSchemeById(@PathVariable Long id) {
        NetRateSchemeResponseDTO dto = netRateSchemeService.getSchemeById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NetRateScheme> updateScheme(
            @PathVariable Long id,
            @RequestBody NetRateSchemeUpdateDTO dto,
            Authentication authentication) {

        // Call the service to update the scheme
        NetRateScheme updatedScheme = netRateSchemeService.updateScheme(id, dto);

        // Return the updated scheme (or just a 204 if you prefer no content)
        return ResponseEntity.ok(updatedScheme);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteSchemes(@RequestBody NetRateSchemeDeleteRequestDTO request) {
        netRateSchemeService.deleteSchemes(request.ids());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{schemeId}/duplicate")
    public ResponseEntity<NetRateSchemeResponseDTO> duplicateScheme(
            @PathVariable Long schemeId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        NetRateScheme duplicated = netRateSchemeService
                .duplicateScheme(schemeId, userDetails.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toDTO(duplicated));
    }

    private NetRateSchemeResponseDTO toDTO(NetRateScheme scheme) {
        List<WorkflowStepRateResponseDTO> wfDtos =
                scheme.getWorkflowStepRates().stream()
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

}
