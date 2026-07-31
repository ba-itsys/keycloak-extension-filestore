/*
 * Copyright 2026 IT-Systemhaus der Bundesagentur fuer Arbeit
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

import static org.assertj.core.api.Assertions.*;

import de.arbeitsagentur.opdt.keycloak.filestore.client.FileClientProvider;
import de.arbeitsagentur.opdt.keycloak.filestore.clientscope.FileClientScopeProvider;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import de.arbeitsagentur.opdt.keycloak.filestore.group.FileGroupProvider;
import de.arbeitsagentur.opdt.keycloak.filestore.identityProvider.FileIdentityProviderStorageProvider;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmProvider;
import de.arbeitsagentur.opdt.keycloak.filestore.role.FileRoleProvider;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;

/**
 * The datastore resolves its SPIs through the default-provider election instead of pinning the file
 * providers by id. The file providers must win the election on every config-store SPI (the tying
 * jpa providers and the infinispan idp cache are disabled by the config defaults). A contributed
 * higher-order provider (e.g. a cassandra area) would win the election instead. That path cannot be
 * exercised here (the embedded server only deploys src/main), it is covered by the
 * keycloak-cassandra-extension test suite.
 */
@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class ProviderResolutionTest extends KeycloakModelTest {

    @TestOnServer
    public void fileProvidersWinTheDefaultProviderElection(KeycloakSession testSession) {
        inCommittedTransaction(testSession, session -> {
            assertThat(session.clients().getClass().getName()).isEqualTo(FileClientProvider.class.getName());
            assertThat(session.realms().getClass().getName()).isEqualTo(FileRealmProvider.class.getName());
            assertThat(session.roles().getClass().getName()).isEqualTo(FileRoleProvider.class.getName());
            assertThat(session.groups().getClass().getName()).isEqualTo(FileGroupProvider.class.getName());
            assertThat(session.clientScopes().getClass().getName()).isEqualTo(FileClientScopeProvider.class.getName());
            assertThat(session.identityProviders().getClass().getName())
                    .isEqualTo(FileIdentityProviderStorageProvider.class.getName());
        });
    }

    @TestOnServer
    public void fileProvidersStayReachableById(KeycloakSession testSession) {
        inCommittedTransaction(testSession, session -> {
            // A routing provider of a conditional area falls back to the file provider by id.
            assertThat(session.getProvider(ClientProvider.class, "file")
                            .getClass()
                            .getName())
                    .isEqualTo(FileClientProvider.class.getName());
        });
    }
}
