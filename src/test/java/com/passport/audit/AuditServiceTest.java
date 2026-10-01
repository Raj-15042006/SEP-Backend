package com.passport.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("Should record genesis audit block when ledger is empty")
    void testRecordGenesisBlock() {
        when(auditLogRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog log = auditService.record("actor-1", "SKILL_CLAIMED", "skill-101", "{\"level\":\"Advanced\"}");

        assertNotNull(log);
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", log.getPrevHash());
        assertNotNull(log.getHash());
        assertEquals(64, log.getHash().length()); // Valid SHA-256 hex length
    }

    @Test
    @DisplayName("Should verify valid cryptographic Merkle chain")
    void testVerifyChainValid() {
        String genesis = "0000000000000000000000000000000000000000000000000000000000000000";
        String hash1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String hash2 = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb";

        AuditLog b1 = AuditLog.builder().id(UUID.randomUUID()).prevHash(genesis).hash(hash1).build();
        AuditLog b2 = AuditLog.builder().id(UUID.randomUUID()).prevHash(hash1).hash(hash2).build();

        when(auditLogRepository.findAll()).thenReturn(List.of(b1, b2));

        AuditService.ChainVerificationReport report = auditService.verifyLedgerChain();

        assertNotNull(report);
        assertTrue(report.isChainValid());
        assertEquals(2, report.getTotalBlocksVerified());
    }

    @Test
    @DisplayName("Should detect tampering and flag invalid chain when a block hash is altered")
    void testVerifyChainTampered() {
        String genesis = "0000000000000000000000000000000000000000000000000000000000000000";
        String hash1 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String tamperedHash2 = "1111111111111111111111111111111111111111111111111111111111111111";
        String hash3 = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb";

        AuditLog b1 = AuditLog.builder().id(UUID.randomUUID()).prevHash(genesis).hash(hash1).build();
        AuditLog b2 = AuditLog.builder().id(UUID.randomUUID()).prevHash("tampered-fake-hash").hash(tamperedHash2).build();
        AuditLog b3 = AuditLog.builder().id(UUID.randomUUID()).prevHash(tamperedHash2).hash(hash3).build();

        when(auditLogRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        AuditService.ChainVerificationReport report = auditService.verifyLedgerChain();

        assertNotNull(report);
        assertFalse(report.isChainValid());
        assertTrue(report.getMessage().contains("BREACH DETECTED"));
    }
}
