package ru.vspochernin.ingestor.processing;

import java.util.Optional;

import ru.vspochernin.ingestor.model.ErrorEvent;

public interface RawEventJsonProcessor {

    Optional<ErrorEvent> process(String rawEventJson);
}
