-- ============================================================================
-- NodeVoltex: Volforce Database Recalculation Trigger (PostgreSQL / Neon)
-- ============================================================================
-- This script sets up an automatic trigger in your PostgreSQL database (Neon)
-- that ensures a player's overall Volforce rating is immediately updated
-- whenever scores are deleted, updated, or inserted directly in the database.
--
-- How to apply this:
-- 1. Open your Neon Console (https://console.neon.tech)
-- 2. Select your project and navigate to the "SQL Editor" tab
-- 3. Copy, paste, and run this entire SQL script
-- ============================================================================

-- 1. Create or replace the recalculation function
CREATE OR REPLACE FUNCTION update_user_volforce_on_score_change()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_new_volforce DOUBLE PRECISION;
BEGIN
    -- Determine which user's overall rating needs recalculation
    IF TG_OP = 'DELETE' THEN
        v_user_id := OLD.user_id;
    ELSE
        v_user_id := NEW.user_id;
    END IF;

    -- Recalculate Volforce: sum the Volforce ratings of the top 10 best plays
    SELECT COALESCE(SUM(volforce), 0.0) INTO v_new_volforce
    FROM (
        SELECT volforce
        FROM scores
        WHERE user_id = v_user_id
        ORDER BY volforce DESC
        LIMIT 10
    ) sub;

    -- Round to 3 decimal places (matching the Spring Boot service specification)
    v_new_volforce := ROUND(v_new_volforce::numeric, 3);

    -- Update the overall volforce rating in the users table
    UPDATE users
    SET volforce = v_new_volforce
    WHERE id = v_user_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 2. Drop the trigger if it already exists to avoid duplication
DROP TRIGGER IF EXISTS trg_update_user_volforce_on_score_change ON scores;

-- 3. Create the AFTER trigger on the scores table
CREATE TRIGGER trg_update_user_volforce_on_score_change
AFTER INSERT OR UPDATE OR DELETE ON scores
FOR EACH ROW
EXECUTE FUNCTION update_user_volforce_on_score_change();
