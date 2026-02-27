package ru.vspochernin.errapi.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.vspochernin.errapi.mapper.ErrorEventRowMapper;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;
import ru.vspochernin.errapi.model.errors.ErrorGroupRow;
import ru.vspochernin.errapi.model.errors.ErrorTimeseriesRow;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.model.errors.TimeBucket;
import ru.vspochernin.errapi.model.errors.Totals;
import ru.vspochernin.errapi.util.ErrorsWhereBuilder;

@Repository
public class ErrorsRepository {

    private static final String TABLE = "errlog_ch.error_events";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ErrorEventRowMapper rowMapper;

    public ErrorsRepository(
            @Qualifier("clickhouseJdbcTemplate")
            NamedParameterJdbcTemplate jdbcTemplate,
            ErrorEventRowMapper rowMapper)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    public long countEvents(ErrorsQuery query) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);

        String sql = """
                SELECT count()
                FROM %s
                WHERE %s
                """.formatted(TABLE, where.sql());

        Long value = jdbcTemplate.queryForObject(sql, where.params(), Long.class);
        return value == null ? 0L : value;
    }

    public List<ErrorEventRow> findEvents(ErrorsQuery query, int limit, long offset) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValues(where.params().getValues());
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        // Намеренно обнуляем stacktrace, чтобы не тянуть его в этом запросе.
        String sql = """
                SELECT
                    event_id,
                    timestamp,
                    source_type,
                    service,
                    level,
                    message_formatted,
                    toString(fingerprint) AS fingerprint_str,
                    fingerprint_source,
                    instance,
                    service_version,
                    logger,
                    thread,
                    message_template,
                    exception_class,
                    exception_message,
                    CAST(NULL, 'Nullable(String)') AS stacktrace
                FROM %s
                WHERE %s
                ORDER BY timestamp DESC, event_id DESC
                LIMIT :limit OFFSET :offset
                """.formatted(TABLE, where.sql());

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public Optional<ErrorEventRow> findEventById(String eventId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("eventId", eventId);

        String sql = """
                SELECT
                    event_id,
                    timestamp,
                    source_type,
                    service,
                    level,
                    message_formatted,
                    toString(fingerprint) AS fingerprint_str,
                    fingerprint_source,
                    instance,
                    service_version,
                    logger,
                    thread,
                    message_template,
                    exception_class,
                    exception_message,
                    stacktrace
                FROM %s
                WHERE event_id = toUUID(:eventId)
                LIMIT 1
                """.formatted(TABLE);

        List<ErrorEventRow> rows = jdbcTemplate.query(sql, params, rowMapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public Totals countEventsAndGroups(ErrorsQuery query) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);

        String sql = """
                SELECT
                    count() AS events_total,
                    uniqExacl(fingerprint) AS groups_total,
                FROM %s
                WHERE %s
                """.formatted(TABLE, where.sql());

        Totals totals = jdbcTemplate.queryForObject(sql, where.params(), (rs, rowNum) ->
                new Totals(rs.getLong("events_total"), rs.getLong("groups_total")));

        return totals == null ? new Totals(0L, 0L) : totals;
    }

    public List<ErrorGroupRow> findGroups(ErrorsQuery query, int limit, long offset) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValues(where.params().getValues());
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        String sql = """
                SELECT
                    toString(fingerprint) AS group_fingerprint,
                    count() AS group_count,
                    max(timestamp) AS group_last_seen,
                
                    toString(argMax(event_id, timestamp)) AS event_id,
                    max(timestamp) AS timestamp,
                    argMax(source_type, timestamp) AS source_type,
                    argMax(service, timestamp) AS service,
                    argMax(level, timestamp) AS level,
                    argMax(message_formatted, timestamp) AS message_formatted,
                    toString(fingerprint) AS fingerprint_str,
                    argMax(fingerprint_source, timestamp) AS fingerprint_source,
                    argMax(instance, timestamp) AS instance,
                    argMax(service_version, timestamp) AS service_version,
                    argMax(logger, timestamp) AS logger,
                    argMax(thread, timestamp) AS thread,
                    argMax(message_template, timestamp) AS message_template,
                    argMax(exception_class, timestamp) AS exception_class,
                    argMax(exception_message, timestamp) AS exception_message,
                    CAST(NULL, 'Nullable(String)') AS stacktrace
                FROM %s
                WHERE %s
                GROUP BY fingerprint
                ORDER BY group_count DESC
                LIMIT :limit OFFSET :offset
                """.formatted(TABLE, where.sql());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp lastSeenTimestamp = rs.getTimestamp("group_last_seen");
            Instant lastSeen = lastSeenTimestamp != null ? lastSeenTimestamp.toInstant() : null;

            String fingerprint = rs.getString("group_fingerprint");
            long count = rs.getLong("group_count");

            ErrorEventRow lastEvent = rowMapper.mapRow(rs, rowNum);
            return new ErrorGroupRow(fingerprint, count, lastSeen, lastEvent);
        });
    }

    public List<ErrorTimeseriesRow> findTimeseries(ErrorsQuery query, TimeBucket bucket) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);
        String intervalSql = bucket.getIntervalSql();

        String sql = """
                SELECT
                    toStartOfInterval(timestamp, %s) AS bucket_start,
                    count() AS bucket_count,
                FROM %s
                WHERE %s
                GROUP BY bucket_start,
                ORDER BY bucket_start ASC
                """.formatted(intervalSql, TABLE, where.sql());

        return jdbcTemplate.query(sql, where.params(), (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("bucket_start");
            Instant bucketStart = timestamp != null ? timestamp.toInstant() : null;
            long count = rs.getLong("bucket_count");
            return new ErrorTimeseriesRow(bucketStart, count);
        });
    }
}
