package ru.vspochernin.ingestor.writer;

import java.util.List;

import ru.vspochernin.ingestor.model.ErrorEvent;

public interface ErrorEventWriter {

    void write(List<ErrorEvent> events);
}
