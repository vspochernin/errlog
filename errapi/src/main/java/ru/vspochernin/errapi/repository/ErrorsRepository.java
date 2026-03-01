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
import ru.vspochernin.errapi.model.errors.EventsAndGroupsTotals;
import ru.vspochernin.errapi.util.ErrorsWhereBuilder;

@Repository
public class ErrorsRepository {

    private static final String TABLE = "errlog_ch.error_events";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ErrorEventRowMapper errorEventRowMapper;

    public ErrorsRepository(
            @Qualifier("clickhouseJdbcTemplate")
            NamedParameterJdbcTemplate jdbcTemplate,
            ErrorEventRowMapper errorEventRowMapper)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.errorEventRowMapper = errorEventRowMapper;
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

        return jdbcTemplate.query(sql, params, errorEventRowMapper);
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

        List<ErrorEventRow> rows = jdbcTemplate.query(sql, params, errorEventRowMapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    public EventsAndGroupsTotals countEventsAndGroupsTotals(ErrorsQuery query) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);

        String sql = """
                SELECT
                    count() AS events_total,
                    uniqExact(fingerprint) AS groups_total
                FROM %s
                WHERE %s
                """.formatted(TABLE, where.sql());

        EventsAndGroupsTotals eventsAndGroupsTotals = jdbcTemplate.queryForObject(sql, where.params(), (rs, rowNum) ->
                new EventsAndGroupsTotals(rs.getLong("events_total"), rs.getLong("groups_total")));

        return eventsAndGroupsTotals == null ? new EventsAndGroupsTotals(0L, 0L) : eventsAndGroupsTotals;
    }

    public List<ErrorGroupRow> findGroups(ErrorsQuery query, int limit, long offset) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query, column -> "src." + column);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValues(where.params().getValues());
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        String sql = """
                SELECT
                    toString(src.fingerprint) AS group_fingerprint,
                    count() AS group_count,
                    max(src.timestamp) AS group_last_seen,
                
                    argMax(src.event_id, tuple(src.timestamp, src.event_id)) AS event_id,
                    argMax(src.timestamp, tuple(src.timestamp, src.event_id)) AS timestamp,
                    argMax(src.source_type, tuple(src.timestamp, src.event_id)) AS source_type,
                    argMax(src.service, tuple(src.timestamp, src.event_id)) AS service,
                    argMax(src.level, tuple(src.timestamp, src.event_id)) AS level,
                    argMax(src.message_formatted, tuple(src.timestamp, src.event_id)) AS message_formatted,
                    toString(src.fingerprint) AS fingerprint_str,
                    argMax(src.fingerprint_source, tuple(src.timestamp, src.event_id)) AS fingerprint_source,
                    argMax(src.instance, tuple(src.timestamp, src.event_id)) AS instance,
                    argMax(src.service_version, tuple(src.timestamp, src.event_id)) AS service_version,
                    argMax(src.logger, tuple(src.timestamp, src.event_id)) AS logger,
                    argMax(src.thread, tuple(src.timestamp, src.event_id)) AS thread,
                    argMax(src.message_template, tuple(src.timestamp, src.event_id)) AS message_template,
                    argMax(src.exception_class, tuple(src.timestamp, src.event_id)) AS exception_class,
                    argMax(src.exception_message, tuple(src.timestamp, src.event_id)) AS exception_message,
                    CAST(NULL, 'Nullable(String)') AS stacktrace
                FROM %s src
                WHERE %s
                GROUP BY src.fingerprint
                ORDER BY group_count DESC, group_last_seen DESC, group_fingerprint DESC
                LIMIT :limit OFFSET :offset
                """.formatted(TABLE, where.sql());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp lastSeenTimestamp = rs.getTimestamp("group_last_seen");
            Instant lastSeen = lastSeenTimestamp != null ? lastSeenTimestamp.toInstant() : null;

            String fingerprint = rs.getString("group_fingerprint");
            long count = rs.getLong("group_count");

            ErrorEventRow lastEvent = errorEventRowMapper.mapRow(rs, rowNum);
            return new ErrorGroupRow(fingerprint, count, lastSeen, lastEvent);
        });
    }

    public List<ErrorTimeseriesRow> findTimeseries(ErrorsQuery query, TimeBucket bucket) {
        ErrorsWhereBuilder.Where where = ErrorsWhereBuilder.buildWhere(query);
        String intervalSql = bucket.getIntervalSql();

        String sql = """
                SELECT
                    toStartOfInterval(timestamp, %s) AS bucket_start,
                    count() AS bucket_count
                FROM %s
                WHERE %s
                GROUP BY bucket_start
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
