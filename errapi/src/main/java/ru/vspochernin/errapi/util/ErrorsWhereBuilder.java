package ru.vspochernin.errapi.util;

import java.sql.Timestamp;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import ru.vspochernin.errapi.exception.ErrapiErrorType;
import ru.vspochernin.errapi.exception.ErrapiException;
import ru.vspochernin.errapi.model.errors.ErrorsFilter;
import ru.vspochernin.errapi.model.errors.ErrorsQuery;

public class ErrorsWhereBuilder {

    private ErrorsWhereBuilder() {
    }

    public static Where buildWhere(ErrorsQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        StringBuilder whereSB = new StringBuilder("timestamp >= :from AND timestamp < :to");
        params.addValue("from", Timestamp.from(query.timeWindow().from()));
        params.addValue("to", Timestamp.from(query.timeWindow().to()));

        query.fingerprintO().ifPresent(fingerprint -> {
            whereSB.append(" AND fingerprint = toUInt64(:fingerprint)");
            params.addValue("fingerprint", fingerprint.toString());
        });

        int n = 0;
        for (ErrorsFilter filter : query.filters()) {
            String column = filter.field().column();
            String paramBase = "filter_" + n;

            switch (filter.operation()) {
                case EQ -> {
                    whereSB.append(" AND ").append(column).append(" = :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                case NE -> {
                    whereSB.append(" AND ").append(column).append(" != :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                case IN -> {
                    whereSB.append(" AND ").append(column).append(" IN (:").append(paramBase).append(")");
                    params.addValue(paramBase, filter.values()); // JDBC сам перечислит список параметров через запятую.
                }
                case LIKE -> {
                    whereSB.append(" AND ").append(column).append(" LIKE :").append(paramBase);
                    params.addValue(paramBase, filter.values().getFirst());
                }
                default -> throw new ErrapiException(
                        ErrapiErrorType.BAD_REQUEST,
                        "Unknown operation: " + filter.operation());
            }
            n++;
        }

        return new Where(whereSB.toString(), params);
    }

    public record Where(String sql, MapSqlParameterSource params) {
    }
}
