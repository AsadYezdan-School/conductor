--prevent transitive dependency cycles in job_dependencies
CREATE FUNCTION fn_prevent_dep_cycle() RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        WITH RECURSIVE reachable(family_id) AS (
            SELECT downstream_family_id
            FROM   job_dependencies
            WHERE  upstream_family_id = NEW.downstream_family_id
            UNION ALL
            SELECT jd.downstream_family_id
            FROM   job_dependencies jd
            JOIN   reachable r ON jd.upstream_family_id = r.family_id
        )
        SELECT 1 FROM reachable WHERE family_id = NEW.upstream_family_id
    ) THEN
        RAISE EXCEPTION
            'dependency cycle: adding edge % -> % would create a cycle',
            NEW.upstream_family_id, NEW.downstream_family_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_no_dep_cycle
    BEFORE INSERT ON job_dependencies
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_dep_cycle();
