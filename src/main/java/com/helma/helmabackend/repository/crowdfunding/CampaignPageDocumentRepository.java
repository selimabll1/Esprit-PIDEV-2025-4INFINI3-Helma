package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.CampaignPageDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CampaignPageDocumentRepository extends JpaRepository<CampaignPageDocument, Long> {

    List<CampaignPageDocument> findByCampaignPageId(Long campaignPageId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CampaignPageDocument d where d.campaignPageId = :campaignPageId")
    void deleteByCampaignPageId(@Param("campaignPageId") Long campaignPageId);
}