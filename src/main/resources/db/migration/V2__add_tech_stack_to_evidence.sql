-- ====================================================================
-- Skills Evidence Passport — Migration V2: Add Tech Stack to Evidence
-- ====================================================================

-- 1. ADD TECH_STACK COLUMN TO EVIDENCE TABLE
ALTER TABLE evidence 
ADD COLUMN IF NOT EXISTS tech_stack TEXT[] DEFAULT '{}';

COMMENT ON COLUMN evidence.tech_stack IS 'List of technologies, frameworks, and programming languages associated with the project evidence';

-- 2. CREATE GIN INDEX FOR HIGH-PERFORMANCE CONTAINMENT SEARCH
-- Enables fast recruiter filtering by tech stack: SELECT * FROM evidence WHERE tech_stack @> ARRAY['React'];
CREATE INDEX IF NOT EXISTS idx_evidence_tech_stack ON evidence USING GIN (tech_stack);
