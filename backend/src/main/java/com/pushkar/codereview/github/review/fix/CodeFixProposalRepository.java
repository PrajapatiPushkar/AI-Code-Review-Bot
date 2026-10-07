package com.pushkar.codereview.github.review.fix;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeFixProposalRepository extends JpaRepository<CodeFixProposal, Long> {

    List<CodeFixProposal> findByFindingIdOrderByCreatedAtDesc(Long findingId);

    Optional<CodeFixProposal> findById(Long id);
}
