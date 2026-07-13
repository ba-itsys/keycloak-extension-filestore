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

package de.arbeitsagentur.opdt.keycloak.filestore.config;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.keycloak.common.Profile;
import org.keycloak.testframework.infinispan.CacheType;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

public class PreExistingFileStoreKeycloakServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        return configure(config, Path.of(KeycloakModelTest.FIXTURE_FILESTORE_DIR));
    }

    protected KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config, Path sourceDir) {
        copyPreExistingFilestore(sourceDir);
        return FileStoreKeycloakServerConfig.currentProjectDependency(config)
                .features(Profile.Feature.STATELESS)
                .cache(CacheType.LOCAL)
                .featuresDisabled(Profile.Feature.AUTHORIZATION, Profile.Feature.ORGANIZATION)
                .option("feature-admin-fine-grained-authz", "disabled")
                .option("spi-datastore--provider", "file")
                .option("spi-map-storage--file--dir", KeycloakModelTest.FIXTURE_TEST_FILESTORE_DIR);
    }

    protected static void copyPreExistingFilestore(Path source) {
        Path target = Path.of(KeycloakModelTest.FIXTURE_TEST_FILESTORE_DIR);
        deleteDirectory(target);
        try (var files = Files.walk(source)) {
            files.forEach(path -> {
                Path targetPath = target.resolve(source.relativize(path));
                try {
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.copy(path, targetPath);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to copy pre-existing filestore path " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to copy pre-existing filestore from " + source + " to " + target, e);
        }
    }

    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to delete pre-existing filestore path " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete pre-existing filestore directory " + directory, e);
        }
    }
}
