package com.helma.helmabackend.repository.crowdfunding;

import com.helma.helmabackend.dto.crowdfunding.ApplicationRaiseSearchCriteria;
import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import org.springframework.data.jpa.domain.Specification;

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
            ApplicationRaiseSearchCriteria c =
                    criteria != null ? criteria : new ApplicationRaiseSearchCriteria();

            boolean needsTagJoin = c.getTag() != null || hasText(c.getSearch());

            SetJoin<ApplicationRaise, ?> tagJoin =
                    needsTagJoin ? root.joinSet("tags", jakarta.persistence.criteria.JoinType.LEFT) : null;

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
            if (c.getStage() != null) {
                predicates.add(cb.equal(root.get("stage"), c.getStage()));
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
            addContains(predicates, cb, root.get("website"), c.getWebsite());
            addContains(predicates, cb, root.get("country"), c.getCountry());
            addContains(predicates, cb, root.get("currency").as(String.class), c.getCurrency());
            addContains(predicates, cb, root.get("summary"), c.getSummary());
            addContains(predicates, cb, root.get("contactFirstName"), c.getContactFirstName());
            addContains(predicates, cb, root.get("contactLastName"), c.getContactLastName());
            addContains(predicates, cb, root.get("contactTitle"), c.getContactTitle());
            addContains(predicates, cb, root.get("contactEmail"), c.getContactEmail());
            addContains(predicates, cb, root.get("contactPhone"), c.getContactPhone());
            addContains(predicates, cb, root.get("problemStatement"), c.getProblemStatement());
            addContains(predicates, cb, root.get("solution"), c.getSolution());
            addContains(predicates, cb, root.get("targetCustomers"), c.getTargetCustomers());
            addContains(predicates, cb, root.get("useOfFunds"), c.getUseOfFunds());
            addContains(predicates, cb, root.get("governorate"), c.getGovernorate());
            addContains(predicates, cb, root.get("city"), c.getCity());

            addRange(predicates, cb, root.get("fundingGoal"), c.getFundingGoalMin(), c.getFundingGoalMax());
            addRange(predicates, cb, root.get("investorsPledgedAmount"), c.getInvestorsPledgedAmountMin(), c.getInvestorsPledgedAmountMax());
            addRange(predicates, cb, root.get("customerCount"), c.getCustomerCountMin(), c.getCustomerCountMax());
            addRange(predicates, cb, root.get("teamSize"), c.getTeamSizeMin(), c.getTeamSizeMax());
            addRange(predicates, cb, root.get("createdAt"), c.getCreatedFrom(), c.getCreatedTo());
            addRange(predicates, cb, root.get("updatedAt"), c.getUpdatedFrom(), c.getUpdatedTo());

            if (hasText(c.getSearch())) {
                String term = likeTerm(c.getSearch());
                List<Predicate> searchPredicates = new ArrayList<>();

                addLikeSearch(searchPredicates, cb, root.get("businessName"), term);
                addLikeSearch(searchPredicates, cb, root.get("website"), term);
                addLikeSearch(searchPredicates, cb, root.get("country"), term);
                addLikeSearch(searchPredicates, cb, root.get("currency").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("summary"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactFirstName"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactLastName"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactTitle"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactEmail"), term);
                addLikeSearch(searchPredicates, cb, root.get("contactPhone"), term);
                addLikeSearch(searchPredicates, cb, root.get("problemStatement"), term);
                addLikeSearch(searchPredicates, cb, root.get("solution"), term);
                addLikeSearch(searchPredicates, cb, root.get("targetCustomers"), term);
                addLikeSearch(searchPredicates, cb, root.get("useOfFunds"), term);
                addLikeSearch(searchPredicates, cb, root.get("governorate"), term);
                addLikeSearch(searchPredicates, cb, root.get("city"), term);
                addLikeSearch(searchPredicates, cb, root.get("stage").as(String.class), term);
                addLikeSearch(searchPredicates, cb, root.get("teamSize").as(String.class), term);

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

                if (!searchPredicates.isEmpty()) {
                    predicates.add(cb.or(searchPredicates.toArray(Predicate[]::new)));
                }
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<ApplicationRaise> hasStatus(ApplicationRaiseStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ApplicationRaise> statusNot(ApplicationRaiseStatus status) {
        return (root, query, cb) -> cb.notEqual(root.get("status"), status);
    }

    private static void addContains(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Expression<String> field,
            String value
    ) {
        if (hasText(value)) {
            predicates.add(cb.like(cb.lower(field), likeTerm(value)));
        }
    }

    private static void addLikeSearch(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Expression<String> field,
            String term
    ) {
        predicates.add(cb.like(cb.lower(field), term));
    }

    private static <T extends Comparable<? super T>> void addRange(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Expression<T> field,
            T min,
            T max
    ) {
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