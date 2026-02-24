package ru.vspochernin.errapi.util;

import java.sql.Timestamp;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;

public class ErrorsWhereBuilder {

    private ErrorsWhereBuilder() {
    }

    public static Where buildWhere(ErrorsQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", Timestamp.from(query.from()));
        params.addValue("to", Timestamp.from(query.to()));

        String where = "timestamp >= :from AND timestamp < :to";
        return new Where(where, params);
    }

    public record Where(String sql, MapSqlParameterSource params) {
    }
}
