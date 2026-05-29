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

import de.arbeitsagentur.opdt.keycloak.filestore.client.FileClientEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.clientscope.FileClientScopeEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.group.FileGroupEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.role.FileRoleEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreExistingFileStoreLoadTest extends KeycloakModelTest {

    private static final Path FIXTURE_ROOT = Path.of(FIXTURE_FILESTORE_DIR);

    @Test
    void whenLoadingPreExistingFilestoreFiles_thenReadOnlyConfigurationIsAvailable() throws IOException {
        FileRealmEntity realm = EntityIO.parseFile(FIXTURE_ROOT.resolve("master.yaml"), FileRealmEntity.class);

        assertThat(realm.getId()).isEqualTo("master");
        assertThat(realm.getName()).isEqualTo("master");
        assertThat(realm.getDisplayName()).isEqualTo("my realm");
        assertThat(realm.getDisplayNameHtml()).contains("my realm");

        assertThat(loadClients())
                .extracting(FileClientEntity::getClientId)
                .contains("account", "admin-cli", "master-realm");
        assertThat(loadClientScopes())
                .extracting(FileClientScopeEntity::getName)
                .contains(
                        "acr",
                        "address",
                        "email",
                        "microprofile-jwt",
                        "offline_access",
                        "phone",
                        "profile",
                        "role_list",
                        "roles",
                        "web-origins");
        assertThat(loadGroups()).extracting(FileGroupEntity::getName).containsExactly("test-group");
        assertThat(loadRoles())
                .extracting(FileRoleEntity::getId)
                .contains(
                        "admin",
                        "create-realm",
                        "default-roles-master",
                        "offline_access",
                        "uma_authorization",
                        "realm-management:manage-users",
                        "master-realm:query-users");
    }

    private static List<FileClientEntity> loadClients() throws IOException {
        return parseAll(FIXTURE_ROOT.resolve("master/clients"), 1, FileClientEntity.class);
    }

    private static List<FileClientScopeEntity> loadClientScopes() throws IOException {
        return parseAll(FIXTURE_ROOT.resolve("master/client-scopes"), 1, FileClientScopeEntity.class);
    }

    private static List<FileGroupEntity> loadGroups() throws IOException {
        return parseAll(FIXTURE_ROOT.resolve("master/groups"), 1, FileGroupEntity.class);
    }

    private static List<FileRoleEntity> loadRoles() throws IOException {
        return parseAll(FIXTURE_ROOT.resolve("master/roles"), 10, FileRoleEntity.class);
    }

    private static <T extends AbstractEntity & UpdatableEntity> List<T> parseAll(
            Path directory, int maxDepth, Class<T> type) throws IOException {
        try (var paths = Files.walk(directory, maxDepth)) {
            return paths.filter(EntityIO::canParseFile)
                    .map(path -> EntityIO.parseFile(path, type))
                    .toList();
        }
    }
}
