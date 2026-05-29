/*
 * Copyright 2024. IT-Systemhaus der Bundesagentur fuer Arbeit
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.arbeitsagentur.opdt.keycloak.filestore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SearchPatternsTest {

    static Stream<Arguments> nullAndNotNullArguments() {
        return Stream.of(arguments(null, null), arguments(null, "not-null"), arguments("not-null", null));
    }

    @ParameterizedTest
    @MethodSource("nullAndNotNullArguments")
    void whenLike_givenAnyNull_thenReturnFalse(String first, String second) {
        var actual = SearchPatterns.like(first, second);
        assertThat(actual).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-pattern", "abc-pattern-xyz", "pattern-xyz", "pattern"})
    void whenLike_givenLowerCaseMatches_thenReturnTrue(String value) {
        var actual = SearchPatterns.like(value, "%pattern%");
        assertThat(actual).isTrue();
    }

    @ParameterizedTest
    @MethodSource("nullAndNotNullArguments")
    void whenInsensitiveLike_givenNull_thenReturnFalse(String first, String second) {
        var actual = SearchPatterns.insensitiveLike(first, second);
        assertThat(actual).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "abc-pattern",
                "abc-PATTERN",
                "abc-Pattern",
                "abc-pattern-xyz",
                "abc-Pattern-xyz",
                "abc-PATTERN-xyz",
                "pattern-xyz",
                "Pattern-xyz",
                "PATTERN-xyz",
                "pattern",
                "Pattern",
                "PATTERN"
            })
    void whenInsensitiveLike_givenMatch_thenReturnTrue(String value) {
        var actual = SearchPatterns.insensitiveLike(value, "%pattern%");
        assertThat(actual).isTrue();
    }
}
