package com.tms.backend.taskList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.TaskListSummaryDTO;
import com.tms.backend.job.Job;
import com.tms.backend.language.Language;
import com.tms.backend.project.Project;
import com.tms.backend.role.RoleConstants;
import com.tms.backend.user.User;
import com.tms.backend.workflowSteps.WorkflowStep;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

// Paginated/filtered/sorted/searchable task-list query backing GET /api/task-lists, mirroring
// JobRowQueryService's approach (filters map keyed the same way the sidebar's activeFilters is
// built). Unlike jobs, a TaskList row doesn't fan out (its jobs collection is only joined for
// filtering/search), so a plain distinct entity Criteria query paginates correctly without the
// tuple-projection workaround JobRowQueryService needs.
@Service
public class TaskListRowQueryService {

    @PersistenceContext
    private EntityManager em;

    private record Joins(
            Join<TaskList, Job> jobs,
            Join<Job, Project> project,
            Join<TaskList, WorkflowStep> workflowStep,
            Join<TaskList, User> assignee,
            Join<TaskList, Language> targetLang) {
    }

    @Transactional(readOnly = true)
    public Page<TaskListSummaryDTO> findTaskLists(User user, int page, int pageSize, String search, String sortBy,
            String sortDir, Map<String, List<String>> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TaskList> cq = cb.createQuery(TaskList.class);
        Root<TaskList> root = cq.from(TaskList.class);
        Joins j = buildJoins(root);

        cq.distinct(true);
        cq.where(buildPredicate(cb, root, j, user, search, filters));
        applySort(cb, cq, root, j, sortBy, sortDir);

        int pageIndex = Math.max(page - 1, 0);
        TypedQuery<TaskList> typedQuery = em.createQuery(cq);
        typedQuery.setFirstResult(pageIndex * pageSize);
        typedQuery.setMaxResults(pageSize);

        List<TaskListSummaryDTO> rows = typedQuery.getResultList().stream()
                .map(TaskListSummaryDTO::from)
                .collect(Collectors.toList());

        long total = countTaskLists(user, search, filters);

        return new PageImpl<>(rows, PageRequest.of(pageIndex, pageSize), total);
    }

    private long countTaskLists(User user, String search, Map<String, List<String>> filters) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TaskList> root = cq.from(TaskList.class);
        Joins j = buildJoins(root);

        cq.select(cb.countDistinct(root));
        cq.where(buildPredicate(cb, root, j, user, search, filters));

        return em.createQuery(cq).getSingleResult();
    }

    private Joins buildJoins(Root<TaskList> root) {
        Join<TaskList, Job> jobs = root.join("jobs", JoinType.LEFT);
        Join<Job, Project> project = jobs.join("project", JoinType.LEFT);
        Join<TaskList, WorkflowStep> workflowStep = root.join("workflowStep", JoinType.LEFT);
        Join<TaskList, User> assignee = root.join("assignee", JoinType.LEFT);
        Join<TaskList, Language> targetLang = root.join("targetLang", JoinType.LEFT);
        return new Joins(jobs, project, workflowStep, assignee, targetLang);
    }

    // Admins see every task list; every other role only sees task lists they are assigned to.
    private Predicate visibleTo(CriteriaBuilder cb, Join<TaskList, User> assignee, User user) {
        if (user.hasAnyRole(RoleConstants.ADMIN)) {
            return cb.conjunction();
        }
        return cb.equal(assignee.get("id"), user.getId());
    }

    private Predicate buildPredicate(CriteriaBuilder cb, Root<TaskList> root, Joins j, User user, String search,
            Map<String, List<String>> filters) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(visibleTo(cb, j.assignee(), user));

        Predicate searchPred = searchPredicate(cb, root, j, search);
        if (searchPred != null) {
            predicates.add(searchPred);
        }

        Predicate filterPred = filterPredicate(cb, root, j, filters);
        if (filterPred != null) {
            predicates.add(filterPred);
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate searchPredicate(CriteriaBuilder cb, Root<TaskList> root, Joins j, String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";

        List<Predicate> ors = new ArrayList<>();
        ors.add(likeLower(cb, root.get("taskName"), pattern));
        ors.add(likeLower(cb, j.project().get("name"), pattern));
        ors.add(likeLower(cb, j.workflowStep().get("name"), pattern));
        ors.add(likeLower(cb, j.targetLang().get("rfcCode"), pattern));
        ors.add(likeLower(cb, assigneeName(cb, j.assignee()), pattern));
        return cb.or(ors.toArray(new Predicate[0]));
    }

    private Predicate filterPredicate(CriteriaBuilder cb, Root<TaskList> root, Joins j,
            Map<String, List<String>> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }
        List<Predicate> ands = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                continue;
            }
            Predicate p = forFilter(cb, root, j, entry.getKey(), values);
            if (p != null) {
                ands.add(p);
            }
        }
        return ands.isEmpty() ? null : cb.and(ands.toArray(new Predicate[0]));
    }

    private Predicate forFilter(CriteriaBuilder cb, Root<TaskList> root, Joins j, String key, List<String> values) {
        return switch (key) {
            case "taskName" -> anyContains(cb, root.get("taskName"), values);
            case "projectName" -> anyContains(cb, j.project().get("name"), values);
            case "workflowStepName" -> anyEqualsLower(cb, j.workflowStep().get("name"), values);
            case "sourceLangCode" -> anyContains(cb, j.project().get("sourceLang"), values);
            case "targetLangCode" -> anyEqualsLower(cb, j.targetLang().get("rfcCode"), values);
            case "assigneeName" -> anyContains(cb, assigneeName(cb, j.assignee()), values);
            case "created" -> anyDateRange(cb, root.get("createDate"), values);
            case "dueDate" -> anyDateRange(cb, root.get("dueDate"), values);
            default -> null;
        };
    }

    private Expression<String> assigneeName(CriteriaBuilder cb, Join<TaskList, User> assignee) {
        Expression<String> first = cb.coalesce(assignee.<String>get("firstName"), "");
        Expression<String> last = cb.coalesce(assignee.<String>get("lastName"), "");
        return cb.concat(cb.concat(first, " "), last);
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

    private void applySort(CriteriaBuilder cb, CriteriaQuery<TaskList> cq, Root<TaskList> root, Joins j,
            String sortBy, String sortDir) {
        Expression<?> sortExpr = switch (sortBy == null ? "" : sortBy) {
            case "taskName" -> (Expression<?>) root.get("taskName");
            case "projectName" -> (Expression<?>) j.project().get("name");
            case "workflowStepName" -> (Expression<?>) j.workflowStep().get("name");
            case "targetLangCode" -> (Expression<?>) j.targetLang().get("rfcCode");
            case "startDate" -> (Expression<?>) root.get("startDate");
            case "dueDate" -> (Expression<?>) root.get("dueDate");
            case "assigneeName" -> assigneeName(cb, j.assignee());
            default -> (Expression<?>) root.get("createDate");
        };

        boolean explicitSort = sortBy != null && !sortBy.isBlank();
        boolean desc = explicitSort ? "desc".equalsIgnoreCase(sortDir) : true;

        cq.orderBy(desc ? cb.desc(sortExpr) : cb.asc(sortExpr));
    }
}
