package ru.vspochernin.ingestor.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RawEventProcessorRegistry {

    // Фиктивный тип для неизвестных системе sourceType.
    static final String UNKNOWN_RAW_EVENT_PROCESSOR_SOURCE_TYPE = "__unknown__";

    private final Map<String, RawEventProcessor> bySourceType;
    private final RawEventProcessor unknownRawEventProcessor;

    public RawEventProcessorRegistry(List<RawEventProcessor> processors) {
        this.bySourceType = processors.stream()
                .collect(Collectors.toMap(
                        RawEventProcessor::sourceType,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(String.format("Duplicate processor %s", a.sourceType()));
                        },
                        HashMap::new));

        this.unknownRawEventProcessor = bySourceType.get(UNKNOWN_RAW_EVENT_PROCESSOR_SOURCE_TYPE);
        if (unknownRawEventProcessor == null) {
            throw new IllegalStateException("Can't find unknown raw event processor");
        }
    }

    public RawEventProcessor getProcessor(String sourceType) {
        if (sourceType == null) {
            return unknownRawEventProcessor;
        }
        return bySourceType.get(sourceType);
    }
}
