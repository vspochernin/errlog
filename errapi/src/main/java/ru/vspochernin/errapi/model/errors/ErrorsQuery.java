package ru.vspochernin.errapi.model.errors;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public record ErrorsQuery(
        ErrorsTimeWindow timeWindow,
        Optional<BigInteger> fingerprintO,
        List<ErrorsFilterCondition> filters)
{
}
