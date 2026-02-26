package ru.vspochernin.errapi.model.errors;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ErrorsQuery(
        Instant from,
        Instant to,
        Optional<BigInteger> fingerprintO,
        List<ErrorsFilterCondition> filters)
{
}
