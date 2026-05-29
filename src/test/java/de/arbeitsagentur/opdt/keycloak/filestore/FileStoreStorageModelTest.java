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
import de.arbeitsagentur.opdt.keycloak.filestore.client.FileClientStore;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmStore;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileStoreStorageModelTest extends KeycloakModelTest {

    private static final String REALM_ID = "storage-smoke";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenRealmAndClientAreCreated_thenWrittenYamlFilesCanBeImportedAgain(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            var storedClient = session.clients().addClient(realm, "stored-client");
            storedClient.addRedirectUri("https://stored-client.example/callback");

            Path filestoreRoot = Path.of(TEST_FILESTORE_DIR);
            Path realmFile = EntityIO.getPathForIdAndParentPath(REALM_ID, filestoreRoot);
            Path clientFile = EntityIO.getPathForIdAndParentPath(
                    "stored-client", filestoreRoot.resolve(REALM_ID).resolve("clients"));

            assertThat(Files.isRegularFile(realmFile)).isTrue();
            assertThat(Files.isRegularFile(clientFile)).isTrue();

            FileRealmEntity writtenRealm = parseWrittenFile(realmFile, FileRealmEntity.class);
            FileClientEntity writtenClient = parseWrittenFile(clientFile, FileClientEntity.class);

            session.realms().removeRealm(realm.getId());
            assertThat(session.realms().getRealmByName(REALM_ID)).isNull();

            FileRealmStore.update(writtenRealm);
            writtenClient.setRealmId(writtenRealm.getId());
            FileClientStore.update(writtenClient);

            var importedRealm = session.realms().getRealmByName(REALM_ID);
            assertThat(importedRealm).isNotNull();
            assertThat(importedRealm.getName()).isEqualTo(REALM_ID);

            var importedClient = session.clients().getClientByClientId(importedRealm, "stored-client");
            assertThat(importedClient).isNotNull();
            assertThat(importedClient.getRedirectUris()).containsExactly("https://stored-client.example/callback");
        });
    }

    private static <T extends AbstractEntity & UpdatableEntity> T parseWrittenFile(Path file, Class<T> type) {
        try {
            Method parseFile = EntityIO.class.getDeclaredMethod("parseFile", Path.class, Class.class);
            parseFile.setAccessible(true);
            return type.cast(parseFile.invoke(null, file, type));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to access filestore parser", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to parse written filestore file " + file, e.getCause());
        }
    }
}
