package com.helma.helmabackend.dto.crowdfunding;

import java.util.ArrayList;
import java.util.List;

public class PortfolioImportResultResponse {
    public Integer totalRows = 0;
    public Integer validRows = 0;
    public Integer invalidRows = 0;
    public Integer importedRows = 0;
    public Boolean replaceExisting = false;
    public String message;
    public List<PortfolioImportRowResponse> rows = new ArrayList<>();
}
