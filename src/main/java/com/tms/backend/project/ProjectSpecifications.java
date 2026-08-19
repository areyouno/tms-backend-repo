package com.tms.backend.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.tms.backend.role.RoleConstants;
import com.tms.backend.user.User;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Builds JPA Specifications for the project list endpoint, mirroring the
 * search/sidebar-filter semantics that used to live client-side in
 * projectStore.ts's filteredProjects getter.
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> visibleTo(User user) {
        // root: the Project entity being queried; 
        // query: the CriteriaQuery under construction;
        // cb: CriteriaBuilder, used to build predicate expressions (WHERE-clause fragments).
        return (root, query, cb) -> {
            Predicate notDeleted = cb.isFalse(root.get("deleted"));
            if (user.hasAnyRole(RoleConstants.ADMIN)) {
                return notDeleted;
            }
            return cb.and(notDeleted, cb.equal(root.get("owner").get("id"), user.getId()));
        };
    }

    public static Specification<Project> search(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Project, ?> client = root.join("client", JoinType.LEFT);
            Join<Project, String> targetLangs = root.join("targetLanguages", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.<String>get("name")), pattern),
                    cb.like(cb.lower(root.<String>get("status")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("sourceLang"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("type"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("createdBy"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(client.<String>get("name"), "")), pattern),
                    cb.like(cb.lower(targetLangs), pattern),
                    cb.like(root.get("id").as(String.class), "%" + term.trim() + "%"));
        };
    }

    public static Specification<Project> fromFilters(Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        Specification<Project> spec = null;
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            Specification<Project> filterSpec = forFilter(entry.getKey(), values);
            if (filterSpec == null) {
                continue;
            }
            spec = spec == null ? filterSpec : spec.and(filterSpec);
        }
        return spec;
    }

    private static Specification<Project> forFilter(String key, List<String> values) {
        return switch (key) {
            case "projectName" -> anyContains(values, "name");
            case "ownerPm", "translator" -> anyContains(values, "createdBy");
            case "progress" -> anyProgressRange(values);
            case "created", "startDate" -> anyDateRange(values, "createDate");
            case "dueDate" -> anyDateRange(values, "dueDate");
            case "sourceLanguage" -> anyContains(values, "sourceLang");
            case "targetLanguage" -> anyTargetLanguage(values);
            case "client" -> anyJoinContains(values, "client", "name");
            case "cost" -> anyContains(values, "purchaseOrderNum");
            case "businessUnit" -> anyJoinContains(values, "businessUnit", "name");
            case "costCenter" -> anyJoinContains(values, "costCenter", "name");
            case "domain" -> anyJoinContains(values, "domain", "name");
            case "subdomain" -> anyJoinContains(values, "subdomain", "name");
            case "type" -> anyContains(values, "type");
            default -> null;
        };
    }

    private static Specification<Project> anyContains(List<String> values, String field) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String val : values) {
                predicates.add(cb.like(cb.lower(cb.coalesce(root.<String>get(field), "")), "%" + val.toLowerCase() + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Project> anyJoinContains(List<String> values, String relation, String field) {
        return (root, query, cb) -> {
            Join<Project, ?> join = root.join(relation, JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            for (String val : values) {
                predicates.add(cb.like(cb.lower(cb.coalesce(join.<String>get(field), "")), "%" + val.toLowerCase() + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Project> anyTargetLanguage(List<String> values) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Project, String> join = root.join("targetLanguages", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            for (String val : values) {
                predicates.add(cb.like(cb.lower(join), "%" + val.toLowerCase() + "%"));
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Project> anyProgressRange(List<String> values) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String val : values) {
                String[] parts = val.split("-");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    BigDecimal min = new BigDecimal(parts[0].trim());
                    BigDecimal max = new BigDecimal(parts[1].trim());
                    predicates.add(cb.between(root.get("progress"), min, max));
                } catch (NumberFormatException ignored) {
                    // unparseable range contributes no match
                }
            }
            return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    private static Specification<Project> anyDateRange(List<String> values, String field) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String val : values) {
                String[] parts = val.split("\\|");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    LocalDateTime start = LocalDate.parse(parts[0].trim()).atStartOfDay();
                    LocalDateTime end = LocalDate.parse(parts[1].trim()).atTime(23, 59, 59);
                    predicates.add(cb.between(root.get(field), start, end));
                } catch (Exception ignored) {
                    // unparseable date range contributes no match
                }
            }
            return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
