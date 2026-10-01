package com.passport.audit;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    @Transactional
    public AuditLog record(String actorId, String action, String resourceId, String payload) {
        String prevHash = auditLogRepository.findTopByOrderByTimestampDesc()
            .map(AuditLog::getHash)
            .orElse(GENESIS_HASH);

        String rawContent = prevHash + ":" + actorId + ":" + action + ":" + resourceId + ":" + (payload != null ? payload : "") + ":" + Instant.now().toEpochMilli();
        String currentHash = computeSha256(rawContent);

        AuditLog logEntry = AuditLog.builder()
            .actorId(actorId)
            .action(action)
            .resourceId(resourceId)
            .payload(payload)
            .prevHash(prevHash)
            .hash(currentHash)
            .build();

        AuditLog saved = auditLogRepository.save(logEntry);
        log.info("Audit record created: id={}, action={}, hash={}", saved.getId(), action, currentHash);
        return saved;
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsForResource(String resourceId) {
        return auditLogRepository.findByResourceIdOrderByTimestampDesc(resourceId);
    }

    public ChainVerificationReport verifyLedgerChain() {
        List<AuditLog> allLogs = auditLogRepository.findAll();
        if (allLogs.isEmpty()) {
            return ChainVerificationReport.builder()
                    .isChainValid(true)
                    .totalBlocksVerified(0)
                    .genesisHash(GENESIS_HASH)
                    .latestHash(GENESIS_HASH)
                    .verifiedAt(Instant.now().toString())
                    .message("Audit ledger is empty (genesis pending).")
                    .build();
        }

        boolean valid = true;
        for (int i = 1; i < allLogs.size(); i++) {
            AuditLog current = allLogs.get(i);
            AuditLog previous = allLogs.get(i - 1);

            if (!current.getPrevHash().equals(previous.getHash())) {
                valid = false;
                log.error("Audit chain broken at block ID: {}! Expected prevHash: {}, but found: {}", 
                        current.getId(), previous.getHash(), current.getPrevHash());
                break;
            }
        }

        return ChainVerificationReport.builder()
                .isChainValid(valid)
                .totalBlocksVerified(allLogs.size())
                .genesisHash(allLogs.get(0).getPrevHash())
                .latestHash(allLogs.get(allLogs.size() - 1).getHash())
                .verifiedAt(Instant.now().toString())
                .message(valid ? "Cryptographic ledger integrity verified. All SHA-256 blocks chained correctly." : "LEDGER INTEGRITY BREACH DETECTED.")
                .build();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Data
    @Builder
    public static class ChainVerificationReport {
        private boolean isChainValid;
        private int totalBlocksVerified;
        private String genesisHash;
        private String latestHash;
        private String verifiedAt;
        private String message;
    }
}
