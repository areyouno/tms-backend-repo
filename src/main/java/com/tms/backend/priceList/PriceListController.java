package com.tms.backend.priceList;

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

import com.tms.backend.dto.PriceListCreateDTO;
import com.tms.backend.dto.PriceListDeleteRequestDTO;
import com.tms.backend.dto.PriceListLanguagePairDTO;
import com.tms.backend.dto.PriceListResponseDTO;
import com.tms.backend.dto.PriceListUpdateDTO;
import com.tms.backend.user.CustomUserDetails;

@RestController
@RequestMapping("/api/price-list")
public class PriceListController {

    private final PriceListService priceListService;

    public PriceListController(PriceListService priceListService) {
        this.priceListService = priceListService;
    }

    @PostMapping("/create")
    public ResponseEntity<PriceListResponseDTO> createPriceList(
            @RequestBody PriceListCreateDTO dto,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        PriceListResponseDTO response = priceListService.createPriceList(dto, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<PriceListResponseDTO>> getAllPriceLists() {
        List<PriceListResponseDTO> dtos = priceListService.getAllPriceLists();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceListResponseDTO> getPriceListById(@PathVariable Long id) {
        PriceListResponseDTO dto = priceListService.getPriceListById(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriceListResponseDTO> updatePriceList(
            @PathVariable Long id,
            @RequestBody PriceListUpdateDTO dto
    ) {
        PriceListResponseDTO response = priceListService.updatePriceList(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deletePriceLists(@RequestBody PriceListDeleteRequestDTO request) {
        priceListService.deletePriceLists(request.ids());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/language-pair")
    public ResponseEntity<PriceListResponseDTO> addLanguagePair(
            @PathVariable Long id,
            @RequestBody PriceListLanguagePairDTO dto
    ) {
        PriceListResponseDTO response = priceListService.addLanguagePair(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/language-pair/{lpId}")
    public ResponseEntity<PriceListResponseDTO> updateLanguagePair(
            @PathVariable Long id,
            @PathVariable Long lpId,
            @RequestBody PriceListLanguagePairDTO dto
    ) {
        PriceListResponseDTO response = priceListService.updateLanguagePair(id, lpId, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/language-pair/{lpId}")
    public ResponseEntity<PriceListResponseDTO> deleteLanguagePair(
            @PathVariable Long id,
            @PathVariable Long lpId
    ) {
        PriceListResponseDTO response = priceListService.deleteLanguagePair(id, lpId);
        return ResponseEntity.ok(response);
    }
}
