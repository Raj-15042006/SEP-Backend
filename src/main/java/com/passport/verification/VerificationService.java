package com.passport.verification;

import com.passport.common.Audited;
import com.passport.common.InputSanitizer;
import com.passport.common.SecurityUtils;
import com.passport.evidence.Evidence;
import com.passport.evidence.EvidenceRepository;
import com.passport.evidence.EvidenceStatus;
import com.passport.rubric.RubricScore;
import com.passport.rubric.RubricScoreRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final EvidenceRepository evidenceRepository;

    @Data
    @Builder
    public static class EvaluationRequest {
        private UUID evidenceId;
        private String decision; // VERIFIED, CHANGES_REQUESTED, REJECTED
        private String comments;
        private List<RubricItemDto> rubricScores;
    }

    @Data
    @Builder
    public static class RubricItemDto {
        private String criterion;
        private Integer assignedScore;
        private Integer maxScore;
        private String comments;
    }

    @Audited(action = "EVIDENCE_EVALUATED", resourceType = "VERIFICATION")
    @Transactional
    public Verification evaluate(EvaluationRequest request) {
        UUID verifierId = SecurityUtils.getCurrentUserId();

        Evidence evidence = evidenceRepository.findById(request.getEvidenceId())
            .orElseThrow(() -> new IllegalArgumentException("Evidence not found with ID: " + request.getEvidenceId()));

        // Separation of Duties & Conflict of Interest Prevention (CWE-863 / Academic Integrity Charter)
        if (verifierId.equals(evidence.getStudentId())) {
            log.warn("Academic integrity violation: Evaluator {} attempted self-verification on evidence {}",
                    verifierId, evidence.getId());
            throw new AccessDeniedException(
                "Academic Integrity Violation: Evaluators are strictly prohibited from evaluating their own evidence submissions."
            );
        }

        int totalScore = request.getRubricScores() != null
            ? request.getRubricScores().stream().mapToInt(RubricItemDto::getAssignedScore).sum() : 0;
        int maxScore = request.getRubricScores() != null
            ? request.getRubricScores().stream().mapToInt(RubricItemDto::getMaxScore).sum() : 0;
        int percentage = maxScore > 0 ? Math.round(((float) totalScore / maxScore) * 100) : 0;

        String level;
        if (percentage >= 90) level = "EXPERT";
        else if (percentage >= 75) level = "ADVANCED";
        else if (percentage >= 60) level = "INTERMEDIATE";
        else if (percentage >= 40) level = "DEVELOPING";
        else level = "BEGINNER";

        // Sanitize feedback comments against Stored XSS (CWE-79)
        String cleanComments = InputSanitizer.sanitizeText(request.getComments());

        Verification verification = Verification.builder()
            .evidenceId(evidence.getId())
            .verifierId(verifierId)
            .decision(request.getDecision())
            .totalScore(totalScore)
            .maxScore(maxScore)
            .percentage(percentage)
            .competencyLevel(level)
            .comments(cleanComments)
            .build();

        Verification saved = verificationRepository.save(verification);

        // Save individual criteria with sanitized criterion comments
        if (request.getRubricScores() != null) {
            for (RubricItemDto item : request.getRubricScores()) {
                RubricScore score = RubricScore.builder()
                    .verificationId(saved.getId())
                    .criterion(InputSanitizer.sanitizeText(item.getCriterion()))
                    .assignedScore(item.getAssignedScore())
                    .maxScore(item.getMaxScore())
                    .criterionComment(InputSanitizer.sanitizeText(item.getComments()))
                    .build();
                rubricScoreRepository.save(score);
            }
        }

        // Update evidence status
        EvidenceStatus newStatus = switch (request.getDecision() != null ? request.getDecision().toUpperCase() : "REJECTED") {
            case "VERIFIED" -> EvidenceStatus.VERIFIED;
            case "CHANGES_REQUESTED" -> EvidenceStatus.CHANGES_REQUESTED;
            default -> EvidenceStatus.REJECTED;
        };

        evidence.setStatus(newStatus);
        evidence.setVerifiedAt(newStatus == EvidenceStatus.VERIFIED ? Instant.now() : null);
        evidenceRepository.save(evidence);

        log.info("Evidence {} evaluated by verifier {} with status: {}", evidence.getId(), verifierId, newStatus);
        return saved;
    }

    public Verification getVerificationForEvidence(UUID evidenceId) {
        return verificationRepository.findByEvidenceId(evidenceId)
            .orElse(null);
    }
}
