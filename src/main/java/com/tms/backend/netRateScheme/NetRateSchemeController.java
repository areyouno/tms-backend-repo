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

import com.tms.backend.dto.NetRateSchemeCreateDTO;
import com.tms.backend.dto.NetRateSchemeDeleteRequestDTO;
import com.tms.backend.dto.NetRateSchemeResponseDTO;
import com.tms.backend.dto.NetRateSchemeUpdateDTO;
import com.tms.backend.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/net-rate-scheme")
@RequiredArgsConstructor
public class NetRateSchemeController {
    private final NetRateSchemeService netRateSchemeService;

    @PostMapping("/create")
    public ResponseEntity<NetRateSchemeResponseDTO> createScheme(
                @RequestBody NetRateSchemeCreateDTO dto,
                Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        NetRateScheme savedScheme = netRateSchemeService.createScheme(dto, userDetails.getId());
        return ResponseEntity.ok(netRateSchemeService.toDTO(savedScheme));
    }

    @GetMapping("/all")
    public ResponseEntity<List<NetRateSchemeResponseDTO>> getAllSchemes() {
        return ResponseEntity.ok(netRateSchemeService.getAllSchemes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NetRateSchemeResponseDTO> getSchemeById(@PathVariable Long id) {
        return ResponseEntity.ok(netRateSchemeService.getSchemeById(id));
    }

    @GetMapping("/default")
    public ResponseEntity<NetRateSchemeResponseDTO> getDefaultScheme() {
        return ResponseEntity.ok(netRateSchemeService.getDefaultScheme());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<NetRateSchemeResponseDTO> getSchemeByClient(@PathVariable long clientId) {
        return ResponseEntity.ok(netRateSchemeService.getSchemeByClientId(clientId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NetRateSchemeResponseDTO> updateScheme(
            @PathVariable Long id,
            @RequestBody NetRateSchemeUpdateDTO dto,
            Authentication authentication) {

        NetRateScheme updatedScheme = netRateSchemeService.updateScheme(id, dto);
        return ResponseEntity.ok(netRateSchemeService.toDTO(updatedScheme));
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id) {
        netRateSchemeService.setDefault(id);
        return ResponseEntity.noContent().build();
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
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        NetRateScheme duplicated = netRateSchemeService.duplicateScheme(schemeId, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(netRateSchemeService.toDTO(duplicated));
    }
}
