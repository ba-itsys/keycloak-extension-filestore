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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.keycloak.common.Version;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.util.JsonSerialization;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
class DefaultRealmBaselineTest extends KeycloakModelTest {

    private static final String UPDATE_BASELINE_PROPERTY = "keycloak.default-realm-baseline.update";
    private static final Path BASELINE_DIR = Path.of("src/test/resources/baselines/keycloak-default-filestore");
    private static final String VERSION_FILE = "keycloak-version.txt";
    private static final Pattern UUID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Test
    void whenKeycloakVersionChanges_thenDefaultFilestoreBaselineChangesAreVisible() throws Exception {
        Map<String, String> actualFilestore = readActualFilestore(Path.of(TEST_FILESTORE_DIR));
        assertThat(actualFilestore).containsKey("master.yaml");

        if (Boolean.getBoolean(UPDATE_BASELINE_PROPERTY)) {
            writeCheckedInBaseline(actualFilestore);
        }

        assertThat(BASELINE_DIR)
                .as("Default filestore baseline is missing. Generate it with -D%s=true", UPDATE_BASELINE_PROPERTY)
                .isDirectory();
        Map<String, JsonNode> actual = toComparableFilestore(actualFilestore);
        Map<String, JsonNode> expected = toComparableFilestore(readCheckedInBaseline());
        assertReadableClientIdentifiers(actual);
        assertReadableClientIdentifiers(expected);
        assertThat(actual.keySet())
                .as(
                        "Keycloak default filestore files changed. Regenerate with -D%s=true if expected.",
                        UPDATE_BASELINE_PROPERTY)
                .containsExactlyElementsOf(expected.keySet());
        for (Map.Entry<String, JsonNode> entry : actual.entrySet()) {
            if (VERSION_FILE.equals(entry.getKey())) {
                continue;
            }
            assertThat(entry.getValue())
                    .as(
                            "Keycloak default filestore file %s changed. Review the diff, then regenerate with -D%s=true if expected.",
                            entry.getKey(), UPDATE_BASELINE_PROPERTY)
                    .isEqualTo(expected.get(entry.getKey()));
        }
        assertThat(actual.get(VERSION_FILE))
                .as(
                        "Keycloak version changed. Regenerate with -D%s=true after reviewing default filestore changes.",
                        UPDATE_BASELINE_PROPERTY)
                .isEqualTo(expected.get(VERSION_FILE));
    }

    private static Map<String, String> readActualFilestore(Path filestoreRoot) throws IOException {
        Map<String, String> actualFilestore = new TreeMap<>();
        actualFilestore.put(VERSION_FILE, Version.VERSION + "\n");
        try (var paths = Files.walk(filestoreRoot)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                String relativePath = filestoreRoot.relativize(file).toString().replace('\\', '/');
                actualFilestore.put(relativePath, normalizeFilestoreContent(Files.readString(file)));
            }
        }
        return actualFilestore;
    }

    private static Map<String, String> readCheckedInBaseline() throws IOException {
        Map<String, String> checkedInBaseline = new TreeMap<>();
        try (var paths = Files.walk(BASELINE_DIR)) {
            for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                String relativePath = BASELINE_DIR.relativize(file).toString().replace('\\', '/');
                checkedInBaseline.put(relativePath, normalizeFilestoreContent(Files.readString(file)));
            }
        }
        return checkedInBaseline;
    }

    private static void writeCheckedInBaseline(Map<String, String> baseline) throws IOException {
        deleteDirectory(BASELINE_DIR);
        for (Map.Entry<String, String> entry : baseline.entrySet()) {
            Path file = BASELINE_DIR.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static Map<String, JsonNode> toComparableFilestore(Map<String, String> filestore) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, JsonNode> comparableFilestore = new TreeMap<>();
        for (Map.Entry<String, String> entry : filestore.entrySet()) {
            if (entry.getKey().endsWith(".yaml")) {
                comparableFilestore.put(
                        entry.getKey(), JsonSerialization.mapper.valueToTree(yaml.load(entry.getValue())));
            } else {
                comparableFilestore.put(entry.getKey(), TextNode.valueOf(entry.getValue()));
            }
        }
        return comparableFilestore;
    }

    private static void assertReadableClientIdentifiers(Map<String, JsonNode> filestore) {
        filestore.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("master/clients/"))
                .filter(entry -> entry.getKey().endsWith(".yaml"))
                .forEach(entry -> {
                    JsonNode client = entry.getValue();
                    assertThat(client.path("clientId").isTextual())
                            .as("Client file %s must use a non-null clientId", entry.getKey())
                            .isTrue();
                    assertThat(client.path("id").asText())
                            .as("Client file %s must use clientId as readable id", entry.getKey())
                            .isEqualTo(client.path("clientId").asText());
                });
    }

    private static String normalizeFilestoreContent(String content) {
        return content.lines()
                        .map(DefaultRealmBaselineTest::normalizeFilestoreLine)
                        .collect(Collectors.joining("\n"))
                + "\n";
    }

    private static String normalizeFilestoreLine(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("privateKey: ")) {
            return line.substring(0, line.indexOf("privateKey: ")) + "privateKey: <generated-private-key>";
        }
        if (trimmed.startsWith("certificate: ")) {
            return line.substring(0, line.indexOf("certificate: ")) + "certificate: <generated-certificate>";
        }
        if (trimmed.startsWith("secret: ") && isGeneratedSecret(trimmed.substring("secret: ".length()))) {
            return line.substring(0, line.indexOf("secret: ")) + "secret: <generated-secret>";
        }

        return UUID.matcher(line).replaceAll("<generated-id>");
    }

    private static boolean isGeneratedSecret(String value) {
        return !"null".equals(value) && !"true".equals(value) && !"false".equals(value);
    }
}
