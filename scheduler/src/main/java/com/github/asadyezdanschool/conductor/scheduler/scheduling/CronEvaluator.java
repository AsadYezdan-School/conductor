package com.github.asadyezdanschool.conductor.scheduler.scheduling;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Parses and evaluates standard 5-field POSIX cron expressions in UTC.
 *
 * <p>Format: {@code <minute> <hour> <dom> <month> <dow>}, e.g. {@code "*&#47;5 * * * *"}.
 * Second-level granularity (6-field Quartz format) is intentionally not supported.
 */
@Singleton
public class CronEvaluator {

    private final CronParser parser;

    @Inject
    public CronEvaluator() {
        this.parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
    }

    /**
     * Compute the next fire time strictly after {@code after}.
     *
     * @param cronExpression standard 5-field POSIX cron
     * @param after          reference instant; result is always strictly later
     * @return next fire time in UTC
     * @throws IllegalArgumentException if {@code cronExpression} is not a valid POSIX cron
     */
    public Instant nextAfter(String cronExpression, Instant after) {
        ZonedDateTime zonedAfter = after.atZone(ZoneOffset.UTC);
        ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
        Optional<ZonedDateTime> next = executionTime.nextExecution(zonedAfter);
        return next
                .orElseThrow(() -> new IllegalStateException(
                        "No next execution found for cron: " + cronExpression))
                .toInstant();
    }

    /**
     * Validate a cron expression without computing a next time.
     *
     * @throws IllegalArgumentException if the expression is syntactically invalid
     */
    public void validate(String cronExpression) {
        try {
            parser.parse(cronExpression).validate();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid POSIX cron expression '" + cronExpression + "': " + e.getMessage(), e);
        }
    }
}
