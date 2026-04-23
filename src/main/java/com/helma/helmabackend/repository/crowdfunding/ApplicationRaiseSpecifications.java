package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseSearchCriteria;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.EquityDetail;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ApplicationRaiseSpecifications {

    private ApplicationRaiseSpecifications() {
    }

    public static Specification<ApplicationRaise> byCriteria(ApplicationRaiseSearchCriteria criteria) {
        return (root, query, cb) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            ApplicationRaiseSearchCriteria c = criteria != null ? criteria : new ApplicationRaiseSearchCriteria();

            boolean needsTagJoin = c.getTag() != null || hasText(c.getSearch());
            boolean needsEquityJoin = hasEquityFilters(c) || hasText(c.getSearch());

            SetJoin<ApplicationRaise, ?> tagJoin = needsTagJoin ? root.joinSet("tags", JoinType.LEFT) : null;
            Join<ApplicationRaise, EquityDetail> equityJoin = needsEquityJoin ? root.join("equityDetail", JoinType.LEFT) : null;

            if (c.getId() != null) {
                predicates.add(cb.equal(root.get("id"), c.getId()));
            }
            if (c.getOwnerUserId() != null) {
                predicates.add(cb.equal(root.get("ownerUserId"), c.getOwnerUserId()));
            }
            if (c.getType() != null) {
                predicates.add(cb.equal(root.get("type"), c.getType()));
            }
            if (c.getSector() != null) {
                predicates.add(cb.equal(root.get("sector"), c.getSector()));
            }
            if (c.getSubSector() != null) {
                predicates.add(cb.equal(root.get("subSector"), c.getSubSector()));
            }
            if (c.getTag() != null && tagJoin != null) {
                predicates.add(cb.equal(tagJoin, c.getTag()));
            }
            if (c.getAcceptedTerms() != null) {
                predicates.add(cb.equal(root.get("acceptedTerms"), c.getAcceptedTerms()));
            }
            if (c.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), c.getStatus()));
            }

            addContains(predicates, cb, root.get("businessName"), c.getBusinessName());
            addContains(predicates, cb, root.get("companyNumber"), c.getCompanyNumber());
            addContains(predicates, cb, root.get("website"), c.getWebsite());
            addContains(predicates, cb, root.get("country"), c.getCountry());
            addContains(predicates, cb, root.get("currency").as(String.class), c.getCurrency());
            addContains(predicates, cb, root.get("summary"), c.getSummary());
            addContains(predicates, cb, root.get("contactFirstName"), c.getContactFirstName());
            addContains(predicates, cb, root.get("contactLastName"), c.getContactLastName());
            addContains(predicates, cb, root.get("contactTitle"), c.getContactTitle());
            addContains(predicates, cb, root.get("contactEmail"), c.getContactEmail());
            addContains(predicates, cb, root.get("contactPhone"), c.getContactPhone());

            addRange(predicates, cb, root.get("fundingGoal"), c.getFundingGoalMin(), c.getFundingGoalMax());
            addRange(predicates, cb, root.get("investorsPledgedAmount"), c.getInvestorsPledgedAmountMin(), c.getInvestorsPledgedAmountMax());
            addRange(predicates, cb, root.get("customerCount"), c.getCustomerCountMin(), c.getCustomerCountMax());
            addRange(predicates, cb, root.get("createdAt"), c.getCreatedFrom(), c.getCreatedTo());
            addRange(predicates, cb, root.get("updatedAt"), c.getUpdatedFrom(), c.getUpdatedTo());

            if (equityJoin != null) {
                addContains(predicates, cb, equityJoin.get("companyLegalName"), c.getCompanyLegalName());
                addContains(predicates, cb, equityJoin.get("companyRegistrationNumber"), c.getCompanyRegistrationNumber());
                addContains(predicates, cb, equityJoin.get("cnreProfileUrl"), c.getCnreProfileUrl());
                addRange(predicates, cb, equityJoin.get("equityOfferedPercent"), c.getEquityOfferedPercentMin(), c.getEquityOfferedPercentMax());
                addRange(predicates, cb, equityJoin.get("preMoneyValuation"), c.getPreMoneyValuationMin(), c.getPreMoneyValuationMax());
                addRange(predicates, cb, equityJoin.get("minInvestment"), c.getMinInvestmentMin(), c.getMinInvestmentMax());
            }

            if (hasText(c.getSearch())) {
                String term = likeTerm(c.getSearch());
                List<Predicate> searchPredicates = new ArrayList<>();

                addLikeSearch(searchPredicates, cb, root.get("businessName"), term);
                addLikeSearch(searchPredicates, cb, root.get("companyNumber"), term);
                addLikeSearch(searchPredicates, cb, root.get("website"), term);
                addLikeSearch(searchPredicates, cb, root.get("country"), term);
                addLikeSearch(searchPredicates, cb, root.get("currency").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("summary"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactFirstName"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactLastName"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactTitle"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactEmail"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactPhone"), term);

                addLikeSearch(searchPredicates, cb, root.get("id").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("ownerUserId").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("type").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("sector").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("subSector").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("status").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("acceptedTerms").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("fundingGoal").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("investorsPledgedAmount").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("customerCount").as(String.class), term);

                if (tagJoin != null) {
                    addLikeSearch(searchPredicates, cb, tagJoin.as(String.class), term);
                }
                if (equityJoin != null) {
                    addLikeSearch(searchPredicates, cb, equityJoin.get("companyLegalName"), term);
                    addLikeSearch(searchPredicates, cb, equityJoin.get("companyRegistrationNumber"), term);
                    addLikeSearch(searchPredicates, cb, equityJoin.get("cnreProfileUrl"), term);
                    addLikeSearch(searchPredicates, cb, equityJoin.get("equityOfferedPercent").as(String.class), term);
                    addLikeSearch(searchPredicates, cb, equityJoin.get("preMoneyValuation").as(String.class), term);
                    addLikeSearch(searchPredicates, cb, equityJoin.get("minInvestment").as(String.class), term);
                }

                if (!searchPredicates.isEmpty()) {
                    predicates.add(cb.or(searchPredicates.toArray(Predicate[]::new)));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<ApplicationRaise> hasStatus(ApplicationRaiseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ApplicationRaise> statusNot(ApplicationRaiseStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    private static boolean hasEquityFilters(ApplicationRaiseSearchCriteria c) {
        return hasText(c.getCompanyLegalName())
                || hasText(c.getCompanyRegistrationNumber())
                || hasText(c.getCnreProfileUrl())
                || c.getEquityOfferedPercentMin() != null
                || c.getEquityOfferedPercentMax() != null
                || c.getPreMoneyValuationMin() != null
                || c.getPreMoneyValuationMax() != null
                || c.getMinInvestmentMin() != null
                || c.getMinInvestmentMax() != null;
    }

    private static void addContains(List<Predicate> predicates,
                                    jakarta.persistence.criteria.CriteriaBuilder cb,
                                    Expression<String> field,
                                    String value) {
        if (hasText(value)) {
            predicates.add(cb.like(cb.lower(field), likeTerm(value)));
        }
    }

    private static void addLikeSearch(List<Predicate> predicates,
                                      jakarta.persistence.criteria.CriteriaBuilder cb,
                                      Expression<String> field,
                                      String term) {
        predicates.add(cb.like(cb.lower(field), term));
    }

    private static <T extends Comparable<? super T>> void addRange(List<Predicate> predicates,
                                                                   jakarta.persistence.criteria.CriteriaBuilder cb,
                                                                   Expression<T> field,
                                                                   T min,
                                                                   T max) {
        if (min != null) {
            predicates.add(cb.greaterThanOrEqualTo(field, min));
        }
        if (max != null) {
            predicates.add(cb.lessThanOrEqualTo(field, max));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String likeTerm(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}