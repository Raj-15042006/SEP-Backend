package com.passport.evidence;

import com.passport.common.Audited;
import com.passport.common.InputSanitizer;
import com.passport.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final FileStorageService fileStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String EVIDENCE_SUBMITTED_TOPIC = "evidence.submitted";

    @Audited(action = "EVIDENCE_SUBMITTED", resourceType = "EVIDENCE")
    @Transactional
    public Evidence submitEvidence(
        UUID skillId,
        String title,
        String type,
        String description,
        String repoUrl,
        String liveUrl,
        String techStack,
        String issuingOrg,
        MultipartFile file
    ) {
        UUID studentId = SecurityUtils.getCurrentUserId();
        String fileRef = fileStorageService.uploadFile(file, studentId);

        // Sanitize all user inputs against Stored XSS and script injection (CWE-79)
        String cleanTitle = InputSanitizer.sanitizeText(title);
        String cleanDescription = InputSanitizer.sanitizeText(description);
        String cleanTechStack = InputSanitizer.sanitizeText(techStack);
        String cleanIssuingOrg = InputSanitizer.sanitizeText(issuingOrg);
        String cleanRepoUrl = InputSanitizer.sanitizeUrl(repoUrl);
        String cleanLiveUrl = InputSanitizer.sanitizeUrl(liveUrl);

        Evidence evidence = Evidence.builder()
            .studentId(studentId)
            .skillId(skillId)
            .title(cleanTitle)
            .type(type)
            .description(cleanDescription)
            .repoUrl(cleanRepoUrl)
            .liveUrl(cleanLiveUrl)
            .techStack(cleanTechStack)
            .issuingOrg(cleanIssuingOrg)
            .fileRef(fileRef)
            .status(EvidenceStatus.SUBMITTED)
            .build();

        Evidence saved = evidenceRepository.save(evidence);

        // Async event publishing to AI-Screening & Verification worker
        try {
            kafkaTemplate.send(EVIDENCE_SUBMITTED_TOPIC, saved.getId().toString(), saved);
            log.info("Kafka event dispatched for evidence: {}", saved.getId());
        } catch (Exception e) {
            log.warn("Kafka event dispatch skipped (Kafka broker offline or dev mode): {}", e.getMessage());
        }

        return saved;
    }

    @Audited(action = "EVIDENCE_UPDATED", resourceType = "EVIDENCE")
    @Transactional
    public Evidence updateEvidence(
        UUID evidenceId,
        String title,
        String description,
        String repoUrl,
        String liveUrl,
        String techStack,
        MultipartFile file
    ) {
        Evidence evidence = getEvidenceById(evidenceId);

        // Enforce Object-Level Authorization (IDOR / BOLA Prevention CWE-639)
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");
        if (!evidence.getStudentId().equals(currentUserId) && !isAdmin) {
            log.warn("Unauthorized attempt by user {} to update evidence {} belonging to student {}",
                    currentUserId, evidenceId, evidence.getStudentId());
            throw new AccessDeniedException("Access denied: You do not possess permission to modify another student's evidence artefact.");
        }

        if (title != null && !title.isBlank()) evidence.setTitle(InputSanitizer.sanitizeText(title));
        if (description != null && !description.isBlank()) evidence.setDescription(InputSanitizer.sanitizeText(description));
        if (repoUrl != null) evidence.setRepoUrl(InputSanitizer.sanitizeUrl(repoUrl));
        if (liveUrl != null) evidence.setLiveUrl(InputSanitizer.sanitizeUrl(liveUrl));
        if (techStack != null) evidence.setTechStack(InputSanitizer.sanitizeText(techStack));
        if (file != null && !file.isEmpty()) {
            evidence.setFileRef(fileStorageService.uploadFile(file, evidence.getStudentId()));
        }
        evidence.setStatus(EvidenceStatus.SUBMITTED);
        return evidenceRepository.save(evidence);
    }

    public List<Evidence> getStudentEvidences(UUID studentId) {
        return evidenceRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);
    }

    public List<Evidence> getVerificationQueue(EvidenceStatus status) {
        if (status == null) {
            return evidenceRepository.findAll();
        }
        return evidenceRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    public Evidence getEvidenceById(UUID id) {
        return evidenceRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Evidence not found with ID: " + id));
    }
}
