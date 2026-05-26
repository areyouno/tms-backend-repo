package com.tms.backend.tomato;

import com.tms.backend.dto.TomatoSizingResponse;

/**
 * Holds the result of submitting a sizing-from-dita job with returnXliff=true.
 * The tomatoJobId can later be used to retrieve the generated XLIFF via
 * /api/Sizing/sizing-xliff/{tomatoJobId}.
 */
public record SizingWithXliffResult(TomatoSizingResponse sizingResponse, String tomatoJobId) {}
