package com.helma.helmabackend.dto.crowdfunding.campaign;

import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

public class CampaignPageUpsertRequest {

    @Size(max = 180, message = "title too long")
    public String title;

    @Size(max = 280, message = "subtitle too long")
    public String subtitle;

    @Size(max = 180, message = "slug too long")
    public String slug;

    @Size(max = 1000, message = "coverMediaUrl too long")
    public String coverMediaUrl;

    /**
     * JSON object containing builder blocks, e.g. {"blocks":[...]}.
     * Keep this as structured JSON, never raw HTML.
     */
    public String contentJson;

    /**
     * JSON object containing controlled visual theme settings.
     */
    public String styleJson;

    /**
     * ApplicationDocument ids selected to be visible on the public campaign page.
     */
    public Set<Long> publicDocumentIds = new LinkedHashSet<>();
}
