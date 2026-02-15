package ru.vspochernin.ingestor.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RawEventProcessorRegistry {

    private final Map<String, RawEventProcessor> bySourceType;
    private final RawEventProcessor defaultRawEventProcessor;

    public RawEventProcessorRegistry(List<RawEventProcessor> processors) {
        this.bySourceType = processors.stream()
                .collect(Collectors.toMap(
                        RawEventProcessor::sourceType,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(String.format("Duplicate processor %s", a.sourceType()));
                        },
                        HashMap::new));

        this.defaultRawEventProcessor = Optional.ofNullable(bySourceType.get(UnknownRawEventProcessor.SOURCE_TYPE))
                .orElseThrow(() -> new IllegalStateException("Can't initialize default raw event processor"));
    }

    public RawEventProcessor getProcessor(String sourceType) {
        return Optional.ofNullable(sourceType)
                .map(bySourceType::get)
                .orElse(defaultRawEventProcessor);
    }
}
