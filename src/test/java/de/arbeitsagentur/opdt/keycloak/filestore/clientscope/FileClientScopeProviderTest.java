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

package de.arbeitsagentur.opdt.keycloak.filestore.clientscope;

import static org.assertj.core.api.Assertions.*;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.Map;
import java.util.stream.Stream;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileClientScopeProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "desert";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenGetClientScopeById_givenUnknownId_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalidId -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScopes, realm) -> {
                // Act
                ClientScopeModel actual = clientScopes.getClientScopeById(realm, invalidId);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetClientScopeById_givenExistingId_thenReturnNotNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Arrange
            clientScopes.addClientScope(realm, "Kalahari");
            // Act
            ClientScopeModel actual = clientScopes.getClientScopeById(realm, "Kalahari");
            // Assert
            assertThat(actual).isNotNull();
        });
    }

    @TestOnServer
    public void whenGetClientScopesStream_givenNoScopes_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Act
            Stream<ClientScopeModel> actual = clientScopes.getClientScopesStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetClientScopesStream_givenClientScopes_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (cs, realm) -> {
            // Arrange
            cs.addClientScope(realm, "Sahara");
            cs.addClientScope(realm, "Kalahari");
            cs.addClientScope(realm, "Gobi");
            // Act
            Stream<ClientScopeModel> actual = cs.getClientScopesStream(realm);
            // Assert
            assertThat(actual)
                    .hasSize(3)
                    .map(ClientScopeModel::getName)
                    .containsExactlyInAnyOrder("Sahara", "Kalahari", "Gobi");
        });
    }

    @TestOnServer
    public void whenAddClientScope_givenNull_then(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Act && Arrange
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> clientScopes.addClientScope(realm, null));
        });
    }

    @TestOnServer
    public void whenAddClientScope_givenExistingId_thenThrowException(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScope, realm) -> {
            // Arrange
            clientScope.addClientScope(realm, "Atacama");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> clientScope.addClientScope(realm, "Atacama"));
        });
    }

    @TestOnServer
    public void whenRemoveClientScope_givenNull_thenReturnFalse(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalidId -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScopes, realm) -> {
                // Act
                boolean actual = clientScopes.removeClientScope(realm, invalidId);
                // Assert
                assertThat(actual).isFalse();
            });
        });
    }

    @TestOnServer
    public void whenRemoveClientScope_givenScope_thenReturnTrue(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Arrange
            clientScopes.addClientScope(realm, "Sonora");
            // Act
            boolean actual = clientScopes.removeClientScope(realm, "Sonora");
            // Assert
            assertThat(actual).isTrue();
        });
    }

    @TestOnServer
    public void whenRemoveClientScopes_givenNoScopes_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Act & Assert
            assertThat(clientScopes.getClientScopesStream(realm)).isEmpty();
            assertThatNoException().isThrownBy(() -> clientScopes.removeClientScopes(realm));
        });
    }

    @TestOnServer
    public void whenRemoveClientScopes_givenScopes_thenScopesAreEmpty(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (clientScopes, realm) -> {
            // Arrange
            clientScopes.addClientScope(realm, "Thar");
            clientScopes.addClientScope(realm, "Namib");
            // Act
            clientScopes.removeClientScopes(realm);
            // Assert
            var actual = clientScopes.getClientScopesStream(realm);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetClientScopesByProtocol_givenClientScopes_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (cs, realm) -> {
            // Arrange
            cs.addClientScope(realm, "Sahara").setProtocol("openid-connect");
            cs.addClientScope(realm, "Kalahari").setProtocol("openid-connect");
            cs.addClientScope(realm, "Gobi").setProtocol("saml");
            // Act
            Stream<ClientScopeModel> actual = cs.getClientScopesByProtocol(realm, "openid-connect");
            // Assert
            assertThat(actual)
                    .hasSize(2)
                    .map(ClientScopeModel::getName)
                    .containsExactlyInAnyOrder("Sahara", "Kalahari");
        });
    }

    @TestOnServer
    public void whenGetClientScopesByAttributes_givenClientScopes_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clientScopes, (cs, realm) -> {
            // Arrange
            Map<String, String> searchMap = Map.of("testKey1", "testVal1", "testKey2", "testVal2");
            Map<String, String> searchMap2 = Map.of("testKey3", "testVal3", "testKey2", "testVal2");

            ClientScopeModel sahara = cs.addClientScope(realm, "Sahara");
            sahara.setAttribute("testKey1", "testVal1");
            sahara.setAttribute("testKey2", "testVal2");

            ClientScopeModel kalahari = cs.addClientScope(realm, "Kalahari");
            kalahari.setAttribute("testKey1", "testVal1");
            kalahari.setAttribute("testKey2", "testVal2");

            ClientScopeModel gobi = cs.addClientScope(realm, "Gobi");
            gobi.setAttribute("testKey2", "testVal2");
            gobi.setAttribute("testKey3", "testVal3");

            Stream<ClientScopeModel> actual = cs.getClientScopesByAttributes(realm, searchMap, false);
            assertThat(actual)
                    .hasSize(2)
                    .map(ClientScopeModel::getName)
                    .containsExactlyInAnyOrder("Sahara", "Kalahari");

            actual = cs.getClientScopesByAttributes(realm, searchMap, true);
            assertThat(actual)
                    .hasSize(3)
                    .map(ClientScopeModel::getName)
                    .containsExactlyInAnyOrder("Sahara", "Kalahari", "Gobi");

            actual = cs.getClientScopesByAttributes(realm, searchMap2, false);
            assertThat(actual).hasSize(1).map(ClientScopeModel::getName).containsExactlyInAnyOrder("Gobi");
        });
    }
}
