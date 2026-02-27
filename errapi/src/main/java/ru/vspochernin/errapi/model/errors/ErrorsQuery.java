package ru.vspochernin.errapi.model.errors;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import ru.vspochernin.errapi.dto.errors.ErrorsRequest;
import ru.vspochernin.errapi.util.ErrorsFiltersParser;
import ru.vspochernin.errapi.util.FingerprintParser;
import ru.vspochernin.errapi.util.TimeWindowParser;

public record ErrorsQuery(
        TimeWindow timeWindow,
        Optional<BigInteger> fingerprintO,
        List<ErrorsFilter> filters)
{

    public static ErrorsQuery parseFromErrorsRequest(ErrorsRequest request) {
        TimeWindow timeWindow = TimeWindowParser.parse(request.from(), request.to());
        Optional<BigInteger> fingerprintO = FingerprintParser.parseO(request.fingerprint());
        List<ErrorsFilter> filters = ErrorsFiltersParser.parse(request.filters());

        return new ErrorsQuery(timeWindow, fingerprintO, filters);
    }
}
