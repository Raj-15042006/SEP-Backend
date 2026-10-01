-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- STUDENT, VERIFIER, RECRUITER, ADMIN
    department VARCHAR(255),
    headline VARCHAR(255),
    bio TEXT,
    college VARCHAR(255),
    degree VARCHAR(255),
    grad_year INT,
    organization_name VARCHAR(255),
    employee_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. SKILL TAXONOMY TABLE
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    domain VARCHAR(255),
    bloom_level VARCHAR(50) DEFAULT 'Synthesis',
    verification_criteria JSONB,
    parent_skill_id UUID REFERENCES skills(id) ON DELETE SET NULL,
    taxonomy_version INT DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. JOB ROLES TABLE
CREATE TABLE IF NOT EXISTS job_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    min_experience_years INT DEFAULT 0,
    target_score INT DEFAULT 85,
    required_skills JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. EVIDENCE TABLE
CREATE TABLE IF NOT EXISTS evidence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- PROJECT, CERTIFICATE, INTERNSHIP, PUBLICATION, OPEN_SOURCE
    description TEXT NOT NULL,
    file_ref VARCHAR(500),
    repo_url VARCHAR(500),
    issuing_org VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED', -- DRAFT, SUBMITTED, UNDER_REVIEW, CHANGES_REQUESTED, VERIFIED, REJECTED
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP WITH TIME ZONE
);

-- 5. AI SCORE ADVISORY TABLE
CREATE TABLE IF NOT EXISTS ai_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evidence_id UUID UNIQUE NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    confidence_score DOUBLE PRECISION NOT NULL,
    duplicate_flag BOOLEAN DEFAULT FALSE,
    rubric_suggestions JSONB,
    model_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. VERIFICATION DECISIONS TABLE
CREATE TABLE IF NOT EXISTS verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    evidence_id UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    verifier_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decision VARCHAR(50) NOT NULL, -- VERIFIED, CHANGES_REQUESTED, REJECTED
    total_score INT NOT NULL,
    max_score INT NOT NULL,
    percentage INT NOT NULL,
    competency_level VARCHAR(50) NOT NULL, -- BEGINNER, DEVELOPING, INTERMEDIATE, ADVANCED, EXPERT
    comments TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. RUBRIC SCORES CRITERIA TABLE
CREATE TABLE IF NOT EXISTS rubric_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    verification_id UUID NOT NULL REFERENCES verifications(id) ON DELETE CASCADE,
    criterion VARCHAR(255) NOT NULL,
    assigned_score INT NOT NULL,
    max_score INT NOT NULL,
    criterion_comment VARCHAR(500)
);

-- 8. STUDENT PORTFOLIO SETTINGS TABLE
CREATE TABLE IF NOT EXISTS portfolios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT TRUE,
    visibility_settings JSONB,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. IMMUTABLE AUDIT LEDGER (HASH-CHAINED)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    payload JSONB,
    prev_hash VARCHAR(64) NOT NULL,
    hash VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES FOR RECRUITER SEARCH & OBSERVABILITY
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_evidence_student ON evidence(student_id);
CREATE INDEX IF NOT EXISTS idx_evidence_skill ON evidence(skill_id);
CREATE INDEX IF NOT EXISTS idx_evidence_status ON evidence(status);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs(actor_id);

-- SEED DATA: HIGH-IMPACT INSTITUTIONAL SKILL TAXONOMY
INSERT INTO skills (id, name, category, domain, bloom_level, description, taxonomy_version) VALUES
('a0000000-0000-0000-0000-000000000001', 'Distributed Systems & Consensus Protocols', 'Systems Architecture', 'Infrastructure & Backend', 'Synthesis', 'Design and implementation of fault-tolerant distributed consensus (Raft/Paxos), leader election, state machine replication, vector clocks, and split-brain mitigation.', 1),
('a0000000-0000-0000-0000-000000000002', 'Cloud-Native Kubernetes & eBPF Orchestration', 'Cloud & Infrastructure', 'DevOps & SRE', 'Evaluation', 'Container lifecycle orchestration, Custom Resource Definitions (CRDs), Cilium eBPF network observability, Service Mesh mTLS, and zero-downtime rolling upgrades.', 1),
('a0000000-0000-0000-0000-000000000003', 'Production LLM Engineering & Advanced RAG', 'Machine Learning & AI', 'Applied AI', 'Synthesis', 'Engineering scalable generative AI pipelines, hybrid dense/sparse vector search (Qdrant/Milvus), contextual chunking, Ragas faithfulness evaluation, and agentic function calling.', 1),
('a0000000-0000-0000-0000-000000000004', 'High-Throughput Event Streaming & Kafka Architecture', 'Data Engineering', 'Distributed Systems', 'Evaluation', 'High-throughput event-driven microservices with Apache Kafka, consumer group partition rebalancing, Transactional Outbox pattern, Debezium CDC, and schema registries.', 1),
('a0000000-0000-0000-0000-000000000005', 'Zero-Trust Security & Cryptographic Verifiability', 'Security & Cryptography', 'Application Security', 'Synthesis', 'Implementation of OAuth2/OIDC token exchange, SPIFFE/SPIRE workload identities, SHA-256 Merkle tree audit ledgers, automated JWT revocation blocklists, and secret rotation.', 1),
('a0000000-0000-0000-0000-000000000006', 'Database Internals, LSM-Trees & Query Optimization', 'Data Systems', 'Core Engineering', 'Analysis', 'PostgreSQL internal storage architecture, WAL logging, B-Tree index selectivity tuning, MVCC concurrency isolation, query execution plan profiling, and table partitioning.', 1),
('a0000000-0000-0000-0000-000000000007', 'Production MLOps & Real-Time Feature Stores', 'Machine Learning & AI', 'Data Platform', 'Evaluation', 'End-to-end ML lifecycle management, automated feature drift detection, Feast online/offline feature synchronization, model quantization with ONNX, and Triton inference serving.', 1),
('a0000000-0000-0000-0000-000000000008', 'Low-Latency Concurrency & Non-Blocking I/O', 'Systems Architecture', 'High Performance', 'Evaluation', 'Modern concurrent systems with Java 21 Virtual Threads (Project Loom), Netty event loops, non-blocking asynchronous reactive streams, and lock-free data structures.', 1),
('a0000000-0000-0000-0000-000000000009', 'Modern Frontend Architecture & Web Performance', 'Frontend Engineering', 'Web Platform', 'Synthesis', 'Institutional-grade React 18/19 architecture, SSR/SSG hydration, Core Web Vitals optimization (LCP < 1.2s, CLS < 0.05), atomic CSS design systems, and WebAssembly integration.', 1),
('a0000000-0000-0000-0000-000000000010', 'Site Reliability Engineering & Chaos Resilience', 'Cloud & Infrastructure', 'Reliability Engineering', 'Analysis', 'SLI/SLO error budget management, OpenTelemetry distributed tracing, Prometheus alerting pipelines, automated runbooks, and Chaos Mesh fault-injection experiments.', 1)
ON CONFLICT (id) DO NOTHING;

-- SEED DATA: AUDIT GENESIS BLOCK
INSERT INTO audit_logs (id, actor_id, action, resource_id, prev_hash, hash) VALUES
('00000000-0000-0000-0000-000000000001', 'SYSTEM_GENESIS', 'LEDGER_INITIALIZED', 'system/genesis', '0000000000000000000000000000000000000000000000000000000000000000', 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855')
ON CONFLICT (id) DO NOTHING;
