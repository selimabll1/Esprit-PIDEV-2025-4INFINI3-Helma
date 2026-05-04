package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.rne.RneShortDetailsResponse;
import com.helma.helmabackend.service.crowdfunding.RneLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crowdfunding/rne")
@RequiredArgsConstructor
public class RneLookupController {

    private final RneLookupService rneLookupService;

    @GetMapping("/short-details/{id}")
    public RneShortDetailsResponse fetchShortDetails(@PathVariable String id) {
        return rneLookupService.fetchShortDetails(id);
    }
}