package com.tms.backend.quote;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.QuoteCreateDTO;
import com.tms.backend.dto.QuoteResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping
    public ResponseEntity<List<QuoteResponseDTO>> getAllQuotes() {
        return ResponseEntity.ok(quoteService.getAllQuotes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponseDTO> getQuote(@PathVariable Long id) {
        return ResponseEntity.ok(quoteService.getQuote(id));
    }

    @PostMapping("/create")
    public ResponseEntity<QuoteResponseDTO> createQuote(@RequestBody QuoteCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quoteService.createQuote(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuote(@PathVariable Long id) {
        quoteService.deleteQuote(id);
        return ResponseEntity.noContent().build();
    }
}
