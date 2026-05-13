package com.helma.helmabackend.controller.kyc;

import com.helma.helmabackend.dto.kyc.KycStatusResponse;
import com.helma.helmabackend.service.kyc.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me/kyc")
@RequiredArgsConstructor
public class UserKycController {

    private final KycService kycService;

    @PostMapping(value = "/cin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycStatusResponse submitCin(@RequestPart("file") MultipartFile file) {
        return kycService.submitCin(file);
    }

    @GetMapping("/status")
    public KycStatusResponse getMyStatus() {
        return kycService.getMyStatus();
    }
}
