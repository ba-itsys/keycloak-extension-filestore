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

public class FileStoreKeycloakServerConfig implements KeycloakServerConfig {

    @Override
    public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
        cleanTestFilestoreDirectory();
        // Select the file datastore + the required stateless feature with an embedded LOCAL cache.
        // The realm/jpa, realm-cache, authorization-cache and organization/infinispan disables are
        // supplied automatically by FileStoreConfigDefaultsSourceFactory, so a green run exercises it.
        return currentProjectDependency(config)
                .features(Profile.Feature.STATELESS)
                .cache(CacheType.LOCAL)
                .featuresDisabled(Profile.Feature.AUTHORIZATION, Profile.Feature.ORGANIZATION)
                .option("feature-admin-fine-grained-authz", "disabled")
                .option("spi-datastore--provider", "file")
                .option("spi-map-storage--file--dir", KeycloakModelTest.TEST_FILESTORE_DIR);
    }

    static KeycloakServerConfigBuilder currentProjectDependency(KeycloakServerConfigBuilder config) {
        try {
            return (KeycloakServerConfigBuilder)
                    config.getClass().getMethod("dependencyCurrentProject").invoke(config);
        } catch (NoSuchMethodException e) {
            return config.dependency("de.arbeitsagentur.opdt", "keycloak-extension-filestore");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to configure current project dependency", e);
        }
    }

    private static void cleanTestFilestoreDirectory() {
        Path filestoreDir = Path.of(KeycloakModelTest.TEST_FILESTORE_DIR);
        if (!Files.exists(filestoreDir)) {
            try {
                Files.createDirectories(filestoreDir);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create test filestore directory " + filestoreDir, e);
            }
            return;
        }
        try (var files = Files.walk(filestoreDir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to clean test filestore path " + path, e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to clean test filestore directory " + filestoreDir, e);
        }
        try {
            Files.createDirectories(filestoreDir);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test filestore directory " + filestoreDir, e);
        }
    }
}
