package com.helma.helmabackend.controller.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.PortfolioDiversificationResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioImportResultResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioOverviewResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioPositionResponse;
import com.helma.helmabackend.dto.crowdfunding.PortfolioSummaryResponse;
import com.helma.helmabackend.service.crowdfunding.PortfolioAnalyticsService;
import com.helma.helmabackend.service.crowdfunding.PortfolioPositionXlsxService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioAnalyticsController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final PortfolioAnalyticsService portfolioAnalyticsService;
    private final PortfolioPositionXlsxService portfolioPositionXlsxService;

    @GetMapping("/me/overview")
    public PortfolioOverviewResponse myOverview() {
        return portfolioAnalyticsService.getMyOverview();
    }

    @GetMapping("/me/summary")
    public PortfolioSummaryResponse mySummary() {
        return portfolioAnalyticsService.getMySummary();
    }

    @GetMapping("/me/positions")
    public List<PortfolioPositionResponse> myPositions() {
        return portfolioAnalyticsService.getMyPositions();
    }

    @GetMapping("/me/diversification")
    public PortfolioDiversificationResponse myDiversification() {
        return portfolioAnalyticsService.getMyDiversification();
    }

    @GetMapping("/me/positions/export-xlsx")
    public ResponseEntity<byte[]> exportMyPositionsXlsx() {
        byte[] file = portfolioPositionXlsxService.exportPositions(portfolioAnalyticsService.getMyPositions());
        return xlsxResponse(file, "helma-portfolio-positions-" + LocalDate.now() + ".xlsx");
    }

    @GetMapping("/me/positions/import-template")
    public ResponseEntity<byte[]> portfolioImportTemplate() {
        byte[] file = portfolioPositionXlsxService.buildImportTemplate();
        return xlsxResponse(file, "helma-portfolio-import-template.xlsx");
    }

    @PostMapping(value = "/me/positions/import-xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PortfolioImportResultResponse importMyPositionsXlsx(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "replaceExisting", defaultValue = "false") boolean replaceExisting
    ) {
        return portfolioPositionXlsxService.importPositions(file, replaceExisting);
    }

    private ResponseEntity<byte[]> xlsxResponse(byte[] file, String filename) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(file);
    }
}
