package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.CampaignPage;
import com.helma.helmabackend.entity.crowdfunding.enums.CampaignPageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignPageRepository extends JpaRepository<CampaignPage, Long> {

    Optional<CampaignPage> findByApplicationRaiseId(Long applicationRaiseId);

    Optional<CampaignPage> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    Optional<CampaignPage> findBySlugIgnoreCase(String slug);

    boolean existsByApplicationRaiseId(Long applicationRaiseId);

    boolean existsBySlugIgnoreCase(String slug);

    List<CampaignPage> findByOwnerUserIdOrderByUpdatedAtDesc(Long ownerUserId);

    List<CampaignPage> findByStatusOrderByUpdatedAtDesc(CampaignPageStatus status);

    List<CampaignPage> findAllByOrderByUpdatedAtDesc();
}
