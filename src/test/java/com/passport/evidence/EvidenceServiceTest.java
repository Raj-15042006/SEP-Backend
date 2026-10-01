package com.passport.evidence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private EvidenceService evidenceService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should submit evidence and transition state to SUBMITTED")
    void testSubmitEvidence() {
        UUID studentId = UUID.randomUUID();
        Authentication auth = new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID skillId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
            "file", "architecture.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        when(fileStorageService.uploadFile(any(), any())).thenReturn("student-id/uuid-architecture.pdf");
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> {
            Evidence e = invocation.getArgument(0);
            return Evidence.builder()
                .id(UUID.randomUUID())
                .studentId(e.getStudentId())
                .skillId(e.getSkillId())
                .title(e.getTitle())
                .type(e.getType())
                .description(e.getDescription())
                .repoUrl(e.getRepoUrl())
                .liveUrl(e.getLiveUrl())
                .techStack(e.getTechStack())
                .issuingOrg(e.getIssuingOrg())
                .fileRef(e.getFileRef())
                .status(EvidenceStatus.SUBMITTED)
                .build();
        });

        Evidence result = evidenceService.submitEvidence(
            skillId,
            "Distributed Storage System",
            "Project",
            "Implemented raft consensus protocol",
            "https://github.com/student/raft",
            "https://raft.example.com",
            "Go, Raft, gRPC",
            "NIT",
            file
        );

        assertNotNull(result);
        assertEquals(EvidenceStatus.SUBMITTED, result.getStatus());
        assertEquals("Distributed Storage System", result.getTitle());
        verify(evidenceRepository, times(1)).save(any(Evidence.class));
    }

    @Test
    @DisplayName("Should filter verification queue by status")
    void testGetVerificationQueue() {
        Evidence e1 = Evidence.builder().id(UUID.randomUUID()).status(EvidenceStatus.SUBMITTED).build();
        when(evidenceRepository.findByStatusOrderBySubmittedAtDesc(EvidenceStatus.SUBMITTED))
            .thenReturn(List.of(e1));

        List<Evidence> queue = evidenceService.getVerificationQueue(EvidenceStatus.SUBMITTED);

        assertEquals(1, queue.size());
        assertEquals(EvidenceStatus.SUBMITTED, queue.get(0).getStatus());
    }

    @Test
    @DisplayName("Should throw exception when evidence ID is not found")
    void testGetEvidenceNotFound() {
        UUID randomId = UUID.randomUUID();
        when(evidenceRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> evidenceService.getEvidenceById(randomId));
    }
}
