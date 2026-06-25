package ru.vspochernin.ingestor.normalization;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownRawEventNormalizerTest {

    private final UnknownRawEventNormalizer normalizer = new UnknownRawEventNormalizer();

    @Test
    void sourceTypeShouldBeUnknown() {
        assertThat(normalizer.sourceType()).isEqualTo("__unknown__");
    }

    @Test
    void normalizeShouldAlwaysReturnEmpty() {
        var node = JsonNodeFactory.instance.objectNode();
        var result = normalizer.normalize(node);

        assertThat(result).isEmpty();
    }

    @Test
    void normalizeShouldReturnEmptyEvenWithFullJson() {
        var node = JsonNodeFactory.instance.objectNode();
        node.put("sourceType", "something");
        node.put("service", "test");

        assertThat(normalizer.normalize(node)).isEmpty();
    }
}
