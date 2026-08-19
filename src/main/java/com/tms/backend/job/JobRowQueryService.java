package com.tms.backend.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.JobRowDTO;
import com.tms.backend.project.Project;
import com.tms.backend.role.RoleConstants;
import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

// Builds the flattened (Job x JobWorkflowStep) row set backing GET /api/jobs: one row per
// workflow step, one row for a job with none. JpaSpecificationExecutor<Job> can't produce this
// safely (it selects the Job root, so pagination would run against pre-fan-out row counts), so
// this uses the Criteria API directly with a Tuple projection over a hand-built join.
@Service
public class JobRowQueryService {

    @PersistenceContext
    private EntityManager em;

    private static final Map<Integer, String> WORKFLOW_STEP_NAMES = Map.ofEntries(
            Map.entry(1, "MT"),
            Map.entry(2, "PE"),
            Map.entry(3, "Translation"),
            Map.entry(4, "MT1"),
            Map.entry(5, "MTPE"),
            Map.entry(6, "Revision"),
            Map.entry(7, "Client review"),
            Map.entry(8, "Quality assessment/LQA"),
            Map.entry(9, "Review"));

    private record Joins(
            Join<Job, JobWorkflowStep> jws,
            Join<JobWorkflowStep, WorkflowStep> ws,
            Join<Job, Project> proj,
            Join<Job, User> owner,
            Join<JobWorkflowStep, User> provider) {
    }

    @Transactional(readOnly = true)
    public Page<JobRowDTO> findJobRows(User user, int page, int pageSize, String search, String sortBy,
            String sortDir, Map<String, List<String>> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Job> root = cq.from(Job.class);
        Joins j = buildJoins(root);

        Expression<String> workflowStepNameExpr = workflowStepNameCase(cb, j.ws().<Integer>get("displayOrder"));
        Expression<String> statusExpr = cb.coalesce(
                j.jws().<JobWorkflowStatus>get("status").as(String.class), JobWorkflowStatus.NEW.name());
        Expression<LocalDateTime> dueDateExpr = cb.coalesce(
                j.jws().<LocalDateTime>get("dueDate"), root.<LocalDateTime>get("createDate"));
        Expression<Long> confirmedPctExpr = cb.coalesce(root.<Long>get("progress"), 0L);

        Predicate predicate = buildPredicate(cb, cq, root, j, workflowStepNameExpr, statusExpr, dueDateExpr, user,
                search, filters);
        cq.where(predicate);

        cq.multiselect(
                root.get("id").alias("id"),
                j.jws().get("id").alias("workflowStepId"),
                root.get("fileName").alias("fileName"),
                j.proj().get("name").alias("project"),
                j.ws().get("displayOrder").alias("stepOrder"),
                workflowStepNameExpr.alias("workflowStepName"),
                statusExpr.alias("status"),
                j.owner().get("firstName").alias("ownerFirstName"),
                j.owner().get("lastName").alias("ownerLastName"),
                j.owner().get("isDeleted").alias("ownerDeleted"),
                root.get("sourceLang").alias("sourceLang"),
                root.get("wordCount").alias("wordCount"),
                root.get("createDate").alias("createDate"),
                dueDateExpr.alias("dueDate"),
                confirmedPctExpr.alias("confirmedPercentage"),
                j.provider().get("firstName").alias("providerFirstName"),
                j.provider().get("lastName").alias("providerLastName"),
                j.provider().get("isDeleted").alias("providerDeleted"),
                j.provider().get("uid").alias("providerUid"));

        Expression<String> ownerName = concatName(cb, j.owner());
        Expression<String> providerName = concatName(cb, j.provider());
        applySort(cb, cq, sortBy, sortDir, root, j, workflowStepNameExpr, statusExpr, dueDateExpr, confirmedPctExpr,
                ownerName, providerName);

        int pageIndex = Math.max(page - 1, 0);
        TypedQuery<Tuple> typedQuery = em.createQuery(cq);
        typedQuery.setFirstResult(pageIndex * pageSize);
        typedQuery.setMaxResults(pageSize);

        List<JobRowDTO> rows = typedQuery.getResultList().stream()
                .map(this::mapTuple)
                .collect(Collectors.toList());
        rows = attachTargetLangs(rows);

        long total = countJobRows(user, search, filters);

        return new PageImpl<>(rows, PageRequest.of(pageIndex, pageSize), total);
    }

    private long countJobRows(User user, String search, Map<String, List<String>> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Job> root = cq.from(Job.class);
        Joins j = buildJoins(root);

        Expression<String> workflowStepNameExpr = workflowStepNameCase(cb, j.ws().<Integer>get("displayOrder"));
        Expression<String> statusExpr = cb.coalesce(
                j.jws().<JobWorkflowStatus>get("status").as(String.class), JobWorkflowStatus.NEW.name());
        Expression<LocalDateTime> dueDateExpr = cb.coalesce(
                j.jws().<LocalDateTime>get("dueDate"), root.<LocalDateTime>get("createDate"));

        Predicate predicate = buildPredicate(cb, cq, root, j, workflowStepNameExpr, statusExpr, dueDateExpr, user,
                search, filters);
        cq.select(cb.count(root)).where(predicate);

        return em.createQuery(cq).getSingleResult();
    }

    private Joins buildJoins(Root<Job> root) {
        Join<Job, JobWorkflowStep> jws = root.join("workflowSteps", JoinType.LEFT);
        Join<JobWorkflowStep, WorkflowStep> ws = jws.join("workflowStep", JoinType.LEFT);
        Join<Job, Project> proj = root.join("project", JoinType.LEFT);
        Join<Job, User> owner = root.join("jobOwner", JoinType.LEFT);
        Join<JobWorkflowStep, User> provider = jws.join("provider", JoinType.LEFT);
        return new Joins(jws, ws, proj, owner, provider);
    }

    private Expression<String> workflowStepNameCase(CriteriaBuilder cb, Expression<Integer> displayOrder) {
        CriteriaBuilder.Case<String> caseExpr = cb.<String>selectCase();
        for (Map.Entry<Integer, String> entry : WORKFLOW_STEP_NAMES.entrySet()) {
            caseExpr = caseExpr.when(cb.equal(displayOrder, entry.getKey()), entry.getValue());
        }
        return caseExpr.otherwise(cb.nullLiteral(String.class));
    }

    private Expression<String> concatName(CriteriaBuilder cb, Join<?, User> userJoin) {
        Expression<String> last = cb.coalesce(userJoin.<String>get("lastName"), "");
        Expression<String> first = cb.coalesce(userJoin.<String>get("firstName"), "");
        return cb.concat(cb.concat(last, " "), first);
    }

    private Predicate visibleTo(CriteriaBuilder cb, Root<Job> root, Join<Job, User> owner, User user) {
        Predicate notDeleted = cb.isFalse(root.get("deleted"));
        if (user.hasAnyRole(RoleConstants.ADMIN, RoleConstants.PM)) {
            return notDeleted;
        }
        return cb.and(notDeleted, cb.equal(owner.get("id"), user.getId()));
    }

    private Predicate buildPredicate(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Job> root, Joins j,
            Expression<String> workflowStepNameExpr, Expression<String> statusExpr,
            Expression<LocalDateTime> dueDateExpr, User user, String search, Map<String, List<String>> filters) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(visibleTo(cb, root, j.owner(), user));

        Predicate searchPred = searchPredicate(cb, cq, root, j, workflowStepNameExpr, statusExpr, search);
        if (searchPred != null) {
            predicates.add(searchPred);
        }

        Predicate filterPred = filterPredicate(cb, cq, root, j, workflowStepNameExpr, statusExpr, dueDateExpr,
                filters);
        if (filterPred != null) {
            predicates.add(filterPred);
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate searchPredicate(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Job> root, Joins j,
            Expression<String> workflowStepNameExpr, Expression<String> statusExpr, String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        Expression<String> ownerName = concatName(cb, j.owner());

        List<Predicate> ors = new ArrayList<>();
        ors.add(likeLower(cb, root.get("fileName"), pattern));
        ors.add(likeLower(cb, ownerName, pattern));
        ors.add(likeLower(cb, root.get("sourceLang"), pattern));
        ors.add(likeLower(cb, workflowStepNameExpr, pattern));
        ors.add(likeLower(cb, statusExpr, pattern));
        ors.add(targetLangExists(cb, cq, root, List.of(search.trim())));
        return cb.or(ors.toArray(new Predicate[0]));
    }

    private Predicate filterPredicate(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Job> root, Joins j,
            Expression<String> workflowStepNameExpr, Expression<String> statusExpr,
            Expression<LocalDateTime> dueDateExpr, Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        List<Predicate> ands = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            Predicate p = forFilter(cb, cq, root, j, workflowStepNameExpr, statusExpr, dueDateExpr, entry.getKey(),
                    values);
            if (p != null) {
                ands.add(p);
            }
        }
        return ands.isEmpty() ? null : cb.and(ands.toArray(new Predicate[0]));
    }

    private Predicate forFilter(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Job> root, Joins j,
            Expression<String> workflowStepNameExpr, Expression<String> statusExpr,
            Expression<LocalDateTime> dueDateExpr, String key, List<String> values) {
        Expression<String> ownerName = concatName(cb, j.owner());
        Expression<String> providerName = concatName(cb, j.provider());
        return switch (key) {
            case "fileName" -> anyContains(cb, root.get("fileName"), values);
            case "jobOwner" -> anyContains(cb, ownerName, values);
            case "project" -> anyContains(cb, j.proj().get("name"), values);
            case "step" -> anyEqualsLower(cb, workflowStepNameExpr, values);
            case "sourceLanguage" -> anyContains(cb, root.get("sourceLang"), values);
            case "targetLanguage" -> targetLangExists(cb, cq, root, values);
            case "provider" -> anyContains(cb, providerName, values);
            case "status" -> anyEqualsLower(cb, statusExpr, values);
            case "created" -> anyDateRange(cb, root.get("createDate"), values);
            case "dueDate" -> anyDateRange(cb, dueDateExpr, values);
            default -> null;
        };
    }

    private Predicate likeLower(CriteriaBuilder cb, Expression<String> expr, String pattern) {
        return cb.like(cb.lower(cb.coalesce(expr, "")), pattern);
    }

    private Predicate anyContains(CriteriaBuilder cb, Expression<String> field, List<String> values) {
        List<Predicate> ors = values.stream()
                .map(v -> cb.like(cb.lower(cb.coalesce(field, "")), "%" + v.toLowerCase() + "%"))
                .collect(Collectors.toList());
        return cb.or(ors.toArray(new Predicate[0]));
    }

    private Predicate anyEqualsLower(CriteriaBuilder cb, Expression<String> field, List<String> values) {
        List<Predicate> ors = values.stream()
                .map(v -> cb.equal(cb.lower(cb.coalesce(field, "")), v.toLowerCase()))
                .collect(Collectors.toList());
        return cb.or(ors.toArray(new Predicate[0]));
    }

    private Predicate anyDateRange(CriteriaBuilder cb, Expression<LocalDateTime> field, List<String> values) {
        List<Predicate> predicates = new ArrayList<>();
        for (String val : values) {
            String[] parts = val.split("\\|");
            if (parts.length != 2) {
                continue;
            }
            try {
                LocalDateTime start = LocalDate.parse(parts[0].trim()).atStartOfDay();
                LocalDateTime end = LocalDate.parse(parts[1].trim()).atTime(23, 59, 59);
                predicates.add(cb.between(field, start, end));
            } catch (Exception ignored) {
                // unparseable date range contributes no match
            }
        }
        return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(new Predicate[0]));
    }

    // Correlated EXISTS against the target-language collection table, so matching a target
    // language never fans the row set out the way a join would.
    private Predicate targetLangExists(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Job> root, List<String> values) {
        Subquery<Long> sub = cq.subquery(Long.class);
        Root<Job> subRoot = sub.correlate(root);
        Join<Job, String> langJoin = subRoot.join("targetLangs", JoinType.INNER);
        List<Predicate> ors = values.stream()
                .map(v -> cb.like(cb.lower(langJoin), "%" + v.toLowerCase() + "%"))
                .collect(Collectors.toList());
        sub.select(cb.literal(1L)).where(cb.or(ors.toArray(new Predicate[0])));
        return cb.exists(sub);
    }

    private void applySort(CriteriaBuilder cb, CriteriaQuery<Tuple> cq, String sortBy, String sortDir,
            Root<Job> root, Joins j, Expression<String> workflowStepNameExpr, Expression<String> statusExpr,
            Expression<LocalDateTime> dueDateExpr, Expression<Long> confirmedPctExpr,
            Expression<String> ownerName, Expression<String> providerName) {

        Expression<?> sortExpr = switch (sortBy == null ? "" : sortBy) {
            case "fileName" -> (Expression<?>) root.get("fileName");
            case "project" -> (Expression<?>) j.proj().get("name");
            case "wordCount" -> (Expression<?>) root.get("wordCount");
            case "dueDate" -> (Expression<?>) dueDateExpr;
            case "status" -> (Expression<?>) statusExpr;
            case "stepOrder" -> (Expression<?>) j.ws().get("displayOrder");
            case "jobOwnerName" -> (Expression<?>) ownerName;
            case "sourceLang" -> (Expression<?>) root.get("sourceLang");
            case "provider" -> (Expression<?>) providerName;
            case "confirmedPercentage" -> (Expression<?>) confirmedPctExpr;
            default -> (Expression<?>) root.get("createDate");
        };

        boolean explicitSort = sortBy != null && !sortBy.isBlank();
        boolean desc = explicitSort ? "desc".equalsIgnoreCase(sortDir) : true;

        cq.orderBy(desc ? cb.desc(sortExpr) : cb.asc(sortExpr));
    }

    private JobRowDTO mapTuple(Tuple t) {
        String status = t.get("status", String.class);
        String jobOwnerName = displayName(
                t.get("ownerFirstName", String.class),
                t.get("ownerLastName", String.class),
                t.get("ownerDeleted", Boolean.class));
        String providerName = displayName(
                t.get("providerFirstName", String.class),
                t.get("providerLastName", String.class),
                t.get("providerDeleted", Boolean.class));

        return new JobRowDTO(
                t.get("id", Long.class),
                t.get("workflowStepId", Long.class),
                t.get("fileName", String.class),
                t.get("project", String.class),
                t.get("stepOrder", Integer.class),
                t.get("workflowStepName", String.class),
                status,
                status,
                jobOwnerName,
                t.get("sourceLang", String.class),
                null,
                t.get("wordCount", Long.class),
                t.get("createDate", LocalDateTime.class),
                t.get("dueDate", LocalDateTime.class),
                t.get("confirmedPercentage", Long.class),
                providerName,
                t.get("providerUid", String.class));
    }

    private static String displayName(String firstName, String lastName, Boolean deleted) {
        if (firstName == null && lastName == null) {
            return null;
        }
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        if (Boolean.TRUE.equals(deleted)) {
            return (last + " (deleted user)").trim();
        }
        return (last + " " + first).trim();
    }

    // targetLangs is excluded from the main tuple projection (joining it there would fan the
    // row set out again, by target language this time). Fetched here in one follow-up query for
    // just the job ids on this page, then merged in.
    private List<JobRowDTO> attachTargetLangs(List<JobRowDTO> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Set<Long> jobIds = rows.stream().map(JobRowDTO::id).collect(Collectors.toSet());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<Job> root = cq.from(Job.class);
        Join<Job, String> langJoin = root.join("targetLangs", JoinType.LEFT);
        cq.multiselect(root.get("id").alias("jobId"), langJoin.alias("lang"));
        cq.where(root.get("id").in(jobIds));

        Map<Long, List<String>> langsByJob = new HashMap<>();
        for (Tuple t : em.createQuery(cq).getResultList()) {
            Long id = t.get("jobId", Long.class);
            String lang = t.get("lang", String.class);
            if (lang == null) {
                continue;
            }
            langsByJob.computeIfAbsent(id, k -> new ArrayList<>()).add(lang);
        }

        return rows.stream()
                .map(r -> new JobRowDTO(r.id(), r.workflowStepId(), r.fileName(), r.project(), r.stepOrder(),
                        r.workflowStepName(), r.status(), r.stepStatus(), r.jobOwnerName(), r.sourceLang(),
                        langsByJob.getOrDefault(r.id(), List.of()), r.wordCount(), r.createDate(), r.dueDate(),
                        r.confirmedPercentage(), r.providerName(), r.providerUid()))
                .collect(Collectors.toList());
    }
}
