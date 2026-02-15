package ru.vspochernin.ingestor.normalization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RawEventNormalizerRegistry {

    private final Map<String, RawEventNormalizer> bySourceType;
    private final RawEventNormalizer defaultRawEventNormalizer;

    public RawEventNormalizerRegistry(List<RawEventNormalizer> normalizers) {
        this.bySourceType = normalizers.stream()
                .collect(Collectors.toMap(
                        RawEventNormalizer::sourceType,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(String.format("Duplicate normalizer %s", a.sourceType()));
                        },
                        HashMap::new));

        this.defaultRawEventNormalizer = Optional.ofNullable(bySourceType.get(UnknownRawEventNormalizer.SOURCE_TYPE))
                .orElseThrow(() -> new IllegalStateException("Can't initialize default raw event normalizer"));
    }

    public RawEventNormalizer getNormalizer(String sourceType) {
        return Optional.ofNullable(sourceType)
                .map(bySourceType::get)
                .orElse(defaultRawEventNormalizer);
    }
}
