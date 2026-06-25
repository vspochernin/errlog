package ru.vspochernin.ingestor.normalization;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import ru.vspochernin.ingestor.model.NormalizedErrorEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawEventNormalizerRegistryTest {

    private final RawEventNormalizer javaNormalizer = new JavaSpringLogbackNormalizerStub();
    private final RawEventNormalizer unknownNormalizer = new UnknownRawEventNormalizer();

    @Test
    void shouldReturnNormalizerBySourceType() {
        var registry = new RawEventNormalizerRegistry(List.of(javaNormalizer, unknownNormalizer));

        assertThat(registry.getNormalizer("java-spring-logback"))
                .isSameAs(javaNormalizer);
    }

    @Test
    void nullSourceTypeShouldReturnDefaultNormalizer() {
        var registry = new RawEventNormalizerRegistry(List.of(javaNormalizer, unknownNormalizer));

        assertThat(registry.getNormalizer(null))
                .isSameAs(unknownNormalizer);
    }

    @Test
    void unknownNonNullSourceTypeShouldReturnDefault() {
        var registry = new RawEventNormalizerRegistry(List.of(javaNormalizer, unknownNormalizer));

        // Unknown normalizer для non-null sourceType возвращает default, а не null
        // (Optional.ofNullable(sourceType).map(...).orElse(default))
        assertThat(registry.getNormalizer("totally-unknown"))
                .isSameAs(unknownNormalizer);
    }

    @Test
    void shouldThrowOnDuplicateSourceType() {
        var duplicateNormalizer = new RawEventNormalizer() {
            @Override
            public String sourceType() {
                return "java-spring-logback"; // дубликат
            }

            @Override
            public Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent) {
                return Optional.empty();
            }
        };

        assertThatThrownBy(() -> new RawEventNormalizerRegistry(
                List.of(javaNormalizer, duplicateNormalizer, unknownNormalizer)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void shouldThrowWhenNoUnknownNormalizer() {
        assertThatThrownBy(() -> new RawEventNormalizerRegistry(List.of(javaNormalizer)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default raw event normalizer");
    }

    /**
     * Стаб с sourceType "java-spring-logback" для тестов registry.
     */
    private static class JavaSpringLogbackNormalizerStub implements RawEventNormalizer {
        @Override
        public String sourceType() {
            return "java-spring-logback";
        }

        @Override
        public Optional<NormalizedErrorEvent> normalize(JsonNode rawEvent) {
            return Optional.empty();
        }
    }
}
