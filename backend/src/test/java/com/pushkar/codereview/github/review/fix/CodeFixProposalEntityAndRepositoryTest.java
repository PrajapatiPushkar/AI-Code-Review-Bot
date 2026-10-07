package com.pushkar.codereview.github.review.fix;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CodeFixProposalEntityAndRepositoryTest {

    @Test
    void testProposalEntityLifecycleAndDefaults() {
        CodeFixProposal proposal = new CodeFixProposal(
                10L,
                "src/main/App.java",
                "Explanation",
                "--- a/App.java\n+++ b/App.java\n@@ -1 +1 @@\n-old\n+new",
                "old",
                "new",
                "Gemini",
                "gemini-3.6-flash",
                "instructions",
                null
        );

        proposal.onCreate();

        assertThat(proposal.getStatus()).isEqualTo(CodeFixProposalStatus.PROPOSED);
        assertThat(proposal.getCreatedAt()).isNotNull();
        assertThat(proposal.getUpdatedAt()).isNotNull();

        Instant initialUpdated = proposal.getUpdatedAt();
        proposal.onUpdate();
        assertThat(proposal.getUpdatedAt()).isAfterOrEqualTo(initialUpdated);
    }

    @Test
    void testRepositoryFindsByFindingIdOrderByCreatedAtDesc() {
        InMemoryCodeFixProposalRepository repository = new InMemoryCodeFixProposalRepository();

        Instant t1 = Instant.parse("2026-10-07T08:00:00Z");
        Instant t2 = Instant.parse("2026-10-07T09:00:00Z");
        Instant t3 = Instant.parse("2026-10-07T10:00:00Z");

        CodeFixProposal p1 = new CodeFixProposal(5L, "src/File.java", "Exp 1", "diff1", "old1", "new1", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        p1.setId(101L);
        p1.setCreatedAt(t1);

        CodeFixProposal p2 = new CodeFixProposal(5L, "src/File.java", "Exp 2", "diff2", "old2", "new2", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        p2.setId(102L);
        p2.setCreatedAt(t3); // Newer

        CodeFixProposal p3 = new CodeFixProposal(5L, "src/File.java", "Exp 3", "diff3", "old3", "new3", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        p3.setId(103L);
        p3.setCreatedAt(t2);

        CodeFixProposal otherFinding = new CodeFixProposal(99L, "src/Other.java", "Exp other", "diff", "o", "n", "Gemini", "m", null, CodeFixProposalStatus.PROPOSED);
        otherFinding.setId(104L);
        otherFinding.setCreatedAt(t3);

        repository.save(p1);
        repository.save(p2);
        repository.save(p3);
        repository.save(otherFinding);

        List<CodeFixProposal> results = repository.findByFindingIdOrderByCreatedAtDesc(5L);

        assertThat(results).hasSize(3);
        // Newest first: p2 (10:00), then p3 (09:00), then p1 (08:00)
        assertThat(results.get(0).getId()).isEqualTo(102L);
        assertThat(results.get(1).getId()).isEqualTo(103L);
        assertThat(results.get(2).getId()).isEqualTo(101L);
    }

    private static class InMemoryCodeFixProposalRepository implements CodeFixProposalRepository {
        private final List<CodeFixProposal> proposals = new ArrayList<>();

        @Override
        public List<CodeFixProposal> findByFindingIdOrderByCreatedAtDesc(Long findingId) {
            return proposals.stream()
                    .filter(p -> p.getFindingId().equals(findingId))
                    .sorted(Comparator.comparing(CodeFixProposal::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public Optional<CodeFixProposal> findById(Long id) {
            return proposals.stream().filter(p -> p.getId().equals(id)).findFirst();
        }

        @Override
        public <S extends CodeFixProposal> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId((long) (proposals.size() + 1));
            }
            proposals.removeIf(p -> p.getId().equals(entity.getId()));
            proposals.add(entity);
            return entity;
        }

        @Override public <S extends CodeFixProposal> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(Long id) { return proposals.stream().anyMatch(p -> p.getId().equals(id)); }
        @Override public List<CodeFixProposal> findAll() { return new ArrayList<>(proposals); }
        @Override public List<CodeFixProposal> findAllById(Iterable<Long> ids) { return null; }
        @Override public long count() { return proposals.size(); }
        @Override public void deleteById(Long id) { proposals.removeIf(p -> p.getId().equals(id)); }
        @Override public void delete(CodeFixProposal entity) { proposals.remove(entity); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { }
        @Override public void deleteAll(Iterable<? extends CodeFixProposal> entities) { }
        @Override public void deleteAll() { proposals.clear(); }
        @Override public void flush() { }
        @Override public <S extends CodeFixProposal> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends CodeFixProposal> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<CodeFixProposal> entities) { }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { }
        @Override public void deleteAllInBatch() { }
        @Override public CodeFixProposal getOne(Long id) { return null; }
        @Override public CodeFixProposal getById(Long id) { return null; }
        @Override public CodeFixProposal getReferenceById(Long id) { return null; }
        @Override public <S extends CodeFixProposal> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends CodeFixProposal> List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends CodeFixProposal> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override public <S extends CodeFixProposal> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends CodeFixProposal> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends CodeFixProposal> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends CodeFixProposal, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<CodeFixProposal> findAll(org.springframework.data.domain.Sort sort) { return null; }
        @Override public org.springframework.data.domain.Page<CodeFixProposal> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}
