package ru.vspochernin.errapi.util;

import java.sql.Timestamp;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsFilterCondition;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;

public class ErrorsWhereBuilder {

    private ErrorsWhereBuilder() {
    }

    public static Where buildWhere(ErrorsQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("from", Timestamp.from(query.from()));
        params.addValue("to", Timestamp.from(query.to()));

        StringBuilder where = new StringBuilder("timestamp >= :from AND timestamp < :to");

        query.fingerprintO().ifPresent(fingerprint -> {
            where.append(" AND fingerprint = toUInt64(:fingerprint)");
            params.addValue("fingerprint", fingerprint.toString());
        });

        int n = 0;
        for (ErrorsFilterCondition filter : query.filters()) {
            String column = filter.field().column();
            String paramBase = "filter_" + n;

            switch (filter.operation()) {
                case EQ -> {
                    where.append(" AND ").append(column).append(" = :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                case NE -> {
                    where.append(" AND ").append(column).append(" != :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                case IN -> {
                    where.append(" AND ").append(column).append(" IN (:").append(paramBase).append(")");
                    params.addValue(paramBase, filter.values());
                }
                case LIKE -> {
                    where.append(" AND ").append(column).append(" LIKE :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                default -> throw new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "Unknown operation: " + filter.operation());
            }
            n++;
        }

        return new Where(where.toString(), params);
    }

    public record Where(String sql, MapSqlParameterSource params) {
    }
}
