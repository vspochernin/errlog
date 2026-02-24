package ru.vspochernin.errapi.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.springframework.jdbc.core.RowMapper;
import ru.vspochernin.errapi.model.errors.ErrorEventRow;

public class ErrorEventRowMapper implements RowMapper<ErrorEventRow> {

    @Override
    public ErrorEventRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp timestamp = rs.getTimestamp("timestamp");
        return new ErrorEventRow(
                rs.getString("event_id"),
                timestamp == null ? null : timestamp.toInstant(),
                rs.getString("source_type"),
                rs.getString("service"),
                rs.getString("level"),
                rs.getString("message_formatted"),
                rs.getString("fingerprint"),
                rs.getString("fingerprint_source"),
                rs.getString("instance"),
                rs.getString("service_version"),
                rs.getString("logger"),
                rs.getString("thread"),
                rs.getString("message_template"),
                rs.getString("exception_class"),
                rs.getString("exception_message"),
                rs.getString("stacktrace"));
    }

}
