-- duration_ms is derivable from (finished_at - started_at); drop the stored column
-- and compute it at query time instead.
ALTER TABLE job_runs DROP COLUMN duration_ms;
