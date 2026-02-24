package ru.vspochernin.errapi.repository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.vspochernin.errapi.mapper.ErrorEventRowMapper;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;
import ru.vspochernin.errapi.util.ErrorsSqlBuilder;

@Repository
@RequiredArgsConstructor
public class ErrorsRepository {

    private static final String TABLE = "errlog_ch.error_events";

    @Qualifier("clickhouseJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ErrorEventRowMapper rowMapper;

    public long countEvents(ErrorsQuery query) {
        ErrorsSqlBuilder.Where where = ErrorsSqlBuilder.buildWhere(query);

        String sql = """
                SELECT count()
                FROM %s
                WHERE %s
                """.formatted(TABLE, where.sql());

        Long value = jdbcTemplate.queryForObject(sql, where.params(), Long.class);
        return value == null ? 0L : value;
    }

    public List<ErrorEventRow> findEvents(ErrorsQuery query, long limit, long offset) {
        ErrorsSqlBuilder.Where where = ErrorsSqlBuilder.buildWhere(query);

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
                    fingerprint,
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
                ORDER BY timestamp DESC
                LIMIT :limit OFFSET :offset
                """.formatted(TABLE, where.sql());

        return jdbcTemplate.query(sql, params, rowMapper);
    }
}
