package com.helma.helmabackend.entity.crowdfunding.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ApplicationTaxonomy {

    private static final Map<Sector, Set<SubSector>> SECTOR_TO_SUBSECTORS = new EnumMap<>(Sector.class);

    static {
        SECTOR_TO_SUBSECTORS.put(Sector.TECHNOLOGY, EnumSet.of(
                SubSector.AI,
                SubSector.SAAS,
                SubSector.CYBERSECURITY,
                SubSector.DATA_ANALYTICS
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.FINTECH_FINANCIAL_SERVICES, EnumSet.of(
                SubSector.PAYMENTS,
                SubSector.EMBEDDED_FINANCE,
                SubSector.ACCOUNTING_FINANCE_SOFTWARE
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.HEALTH_BIO, EnumSet.of(
                SubSector.DIGITAL_HEALTH,
                SubSector.WELLNESS,
                SubSector.MENTAL_HEALTH
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.CONSUMER_PRODUCTS, EnumSet.of(
                SubSector.FOOD_BEVERAGE,
                SubSector.FASHION_APPAREL,
                SubSector.BEAUTY_PERSONAL_CARE
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.COMMERCE_MARKETPLACES, EnumSet.of(
                SubSector.D2C_BRANDS,
                SubSector.ONLINE_MARKETPLACE,
                SubSector.ECOMMERCE_ENABLEMENT
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.CLIMATE_ENERGY, EnumSet.of(
                SubSector.CLEAN_ENERGY,
                SubSector.CLIMATE_SOFTWARE,
                SubSector.SUSTAINABLE_MATERIALS
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.REAL_ESTATE_BUILT_ENVIRONMENT, EnumSet.of(
                SubSector.PROPTECH
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.MOBILITY_INDUSTRY_LOGISTICS, EnumSet.of(
                SubSector.LOGISTICSTECH,
                SubSector.INDUSTRIAL_AUTOMATION
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.EDUCATION_WORK, EnumSet.of(
                SubSector.FUTURE_OF_WORK,
                SubSector.PRODUCTIVITY_TOOLS
        ));

        SECTOR_TO_SUBSECTORS.put(Sector.MEDIA_COMMUNITY_LEISURE, EnumSet.of(
                SubSector.GAMING,
                SubSector.CREATOR_ECONOMY
        ));
    }

    private ApplicationTaxonomy() {}

    public static boolean isValid(Sector sector, SubSector subSector) {
        if (sector == null || subSector == null) return false;
        return SECTOR_TO_SUBSECTORS.getOrDefault(sector, EnumSet.noneOf(SubSector.class))
                .contains(subSector);
    }

    public static Set<SubSector> getAllowedSubSectors(Sector sector) {
        return SECTOR_TO_SUBSECTORS.getOrDefault(sector, EnumSet.noneOf(SubSector.class));
    }
}