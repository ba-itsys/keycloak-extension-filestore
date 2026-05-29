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

package de.arbeitsagentur.opdt.keycloak.filestore.client;

import static org.assertj.core.api.Assertions.*;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileClientProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "capital";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenGetClientsStream_givenNoClients_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act
            Stream<ClientModel> actual = clients.getClientsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetClientById_givenUnknownId_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(unknownId -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
                // Act
                ClientModel actual = clients.getClientById(realm, unknownId);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetClientById_givenClient_thenReturnNotNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Nassau");
            // Act
            ClientModel actual = clients.getClientById(realm, "Nassau");
            // Assert
            assertThat(actual).isNotNull();
        });
    }

    @TestOnServer
    public void whenAddClient_givenExplicitId_thenClientIdIsUsedAsReadableIdentifier(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            ClientModel actual = clients.addClient(realm, "generated-id", "Readable Client");

            assertThat(actual.getId()).isEqualTo("Readable Client");
            assertThat(actual.getClientId()).isEqualTo("Readable Client");
            assertThat(clients.getClientById(realm, "Readable Client")).isNotNull();
            assertThat(clients.getClientById(realm, "generated-id")).isNull();
        });
    }

    @TestOnServer
    public void whenGetClientByClientId_givenUnknownClientId_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(unknownClientId -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
                // Act
                ClientModel actual = clients.getClientByClientId(realm, unknownClientId);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetClientByClientId_givenExistingClientId_thenReturnClient(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "London");
            // Act
            ClientModel actual = clients.getClientByClientId(realm, "London");
            // Assert
            assertThat(actual.getClientId()).isEqualTo("London");
        });
    }

    @TestOnServer
    public void whenGetClientsStream_givenNoClient_thenStreamIsEmpty(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act
            Stream<ClientModel> actual = clients.getClientsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetClientsStream_givenClients_thenReturnStreamWithClients(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "London");
            clients.addClient(realm, "Dublin");
            clients.addClient(realm, "Paris");
            // Act
            Stream<ClientModel> actual = clients.getClientsStream(realm);
            // Assert
            assertThat(actual)
                    .hasSize(3)
                    .map(ClientModel::getClientId)
                    .containsExactlyInAnyOrder("London", "Dublin", "Paris");
        });
    }

    @TestOnServer
    public void whenGetClientsStreamWithPagination_givenClients_thenReturnPaginatedStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            // Note: the list will be sorted alphabetically to get a consistent pagination
            clients.addClient(realm, "Amsterdam");
            clients.addClient(realm, "Berlin");
            clients.addClient(realm, "Dublin"); // <-- idx=2
            clients.addClient(realm, "London");
            clients.addClient(realm, "Oslo"); // <-- maxResults=3
            clients.addClient(realm, "Stockholm");
            // Act
            Stream<ClientModel> actual = clients.getClientsStream(realm, 2, 3);
            // Assert
            assertThat(actual)
                    .hasSize(3)
                    .map(ClientModel::getClientId)
                    .containsExactlyInAnyOrder("Dublin", "London", "Oslo");
        });
    }

    @TestOnServer
    public void whenAddClient_givenNull_thenIllegalArgumentExceptionIsThrown(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Act & Assert
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> clients.addClient(realm, null, null));
        });
    }

    @TestOnServer
    public void whenAddClient_givenExistingClient_thenThrowException(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "warsaw", "Hamburg");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> clients.addClient(realm, null, "Hamburg"));
        });
    }

    @TestOnServer
    public void whenAddClient_givenExistingExplicitId_thenThrowException(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            clients.addClient(realm, "Existing Client");

            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> clients.addClient(realm, "Existing Client", "Different Client"));
        });
    }

    @TestOnServer
    public void whenGetAlwaysDisplayInConsoleClientsStream_givenNoClients_thenReturnEmptyStream(
            KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act
            Stream<ClientModel> actual = clients.getAlwaysDisplayInConsoleClientsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetAlwaysDisplayInConsoleClientsStream_givenNoMatchingClients_thenReturnEmptyStream(
            KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Windhoek").setAlwaysDisplayInConsole(false); // will be excluded
            // Act
            Stream<ClientModel> actual = clients.getAlwaysDisplayInConsoleClientsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetAlwaysDisplayInConsoleClientsStream_givenClients_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Zagreb").setAlwaysDisplayInConsole(true);
            clients.addClient(realm, "Windhoek").setAlwaysDisplayInConsole(false); // will be excluded
            clients.addClient(realm, "Amsterdam").setAlwaysDisplayInConsole(true);
            // Act
            Stream<ClientModel> actual = clients.getAlwaysDisplayInConsoleClientsStream(realm);
            // Assert
            assertThat(actual).hasSize(2).map(ClientModel::getClientId).containsExactly("Amsterdam", "Zagreb");
        });
    }

    @TestOnServer
    public void whenGetClientsCount_givenNoClients_thenReturnZero(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act
            long actual = clients.getClientsCount(realm);
            // Assert
            assertThat(actual).isZero();
        });
    }

    @TestOnServer
    public void whenGetClientsCount_givenClients_thenReturnCount(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Vienna");
            clients.addClient(realm, "Rome");
            clients.addClient(realm, "Oslo");
            // Act
            long actual = clients.getClientsCount(realm);
            // Assert
            assertThat(actual).isEqualTo(3);
        });
    }

    @TestOnServer
    public void whenRemoveClients_givenNoClients_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act & Assert
            assertThatNoException().isThrownBy(() -> clients.removeClients(realm));
        });
    }

    @TestOnServer
    public void whenRemoveClients_givenSingleClient_thenClientIsRemoved(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Nairobi");
            // Act
            clients.removeClients(realm);
            // Assert
            var actual = clients.getClientsStream(realm);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveClients_givenMultipleClients_thenClientIsRemoved(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Madrid");
            clients.addClient(realm, "Lisbon");
            clients.addClient(realm, "Lima");
            // Act
            clients.removeClients(realm);
            // Assert
            var actual = clients.getClientsStream(realm);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveClient_givenNull_thenReturnFalse(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
                // Act
                var actual = clients.removeClient(realm, invalid);
                // Assert
                assertThat(actual).isFalse();
            });
        });
    }

    @TestOnServer
    public void whenRemoveClient_givenClient_thenReturnTrue(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Jakarta");
            // Act
            var actual = clients.removeClient(realm, "Jakarta");
            // Assert
            assertThat(actual).isTrue();
        });
    }

    @TestOnServer
    public void whenSearchClientsByClientIdStream_givenNoClients_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Toronto"); // no match -> exclude
            clients.addClient(realm, "patty"); // partial match -> exclude
            // Act
            Stream<ClientModel> actual = clients.searchClientsByClientIdStream(realm, "pattern", null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchClientsByClientIdStream_givenNullParam_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Act
            Stream<ClientModel> actual = clients.searchClientsByClientIdStream(realm, null, null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchClientsByClientIdStream_givenMatchingClients_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Toronto"); // no match -> exclude
            clients.addClient(realm, "patty"); // partial match -> exclude
            clients.addClient(realm, "abc pattern"); // end
            clients.addClient(realm, "abc pattern xyz"); // middle
            clients.addClient(realm, "pattern"); // exact
            clients.addClient(realm, "pattern xyz"); // start
            // Act
            Stream<ClientModel> actual = clients.searchClientsByClientIdStream(realm, "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(ClientModel::getClientId)
                    .containsExactly("abc pattern", "abc pattern xyz", "pattern", "pattern xyz");
        });
    }

    @TestOnServer
    public void whenSearchClientsByClientIdStream_givenResultLimits_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "0 pattern");
            clients.addClient(realm, "1 pattern"); // start, firstResult = 1
            clients.addClient(realm, "2 pattern"); // end, maxResults = 2
            clients.addClient(realm, "3 pattern");
            // Act
            Stream<ClientModel> actual = clients.searchClientsByClientIdStream(realm, "pattern", 1, 2);
            // Assert
            assertThat(actual).hasSize(2).map(ClientModel::getClientId).containsExactly("1 pattern", "2 pattern");
        });
    }

    @TestOnServer
    public void whenSearchClientsByClientIdStream_givenOutOfBoundLimits_thenReturnEmptyStream(
            KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "0 pattern");
            // Act
            Stream<ClientModel> actual = clients.searchClientsByClientIdStream(realm, "pattern", 5, 10);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchClientsByAttributes_givenNoClients_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            Stream<ClientModel> actual = clients.searchClientsByAttributes(realm, Map.of(), null, null);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchClientsByAttributes_givenNoMatch_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Vilnius").setAttribute("no-match", "val");
            // Act
            Stream<ClientModel> actual = clients.searchClientsByAttributes(realm, Map.of("match", "val"), null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchClientsByAttributes_givenClients_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "Vilnius").setAttribute("no-match", "val");
            clients.addClient(realm, "Zagreb").setAttribute("match", "val");
            clients.addClient(realm, "Albuquerque").setAttribute("match", "val");
            // Act
            Stream<ClientModel> actual = clients.searchClientsByAttributes(realm, Map.of("match", "val"), null, null);
            // Assert
            assertThat(actual).hasSize(2).map(ClientModel::getClientId).containsExactly("Albuquerque", "Zagreb");
        });
    }

    @TestOnServer
    public void whenSearchClientsByAttributes_givenClientWithMultipleAttrs_thenReturnStream(
            KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            var c = clients.addClient(realm, "Vilnius");
            c.setAttribute("no-match", "val");
            c.setAttribute("match", "val");
            // Act
            Stream<ClientModel> actual = clients.searchClientsByAttributes(realm, Map.of("match", "val"), null, null);
            // Assert
            assertThat(actual).hasSize(1).map(ClientModel::getClientId).containsExactly("Vilnius");
        });
    }

    @TestOnServer
    public void whenSearchClientsByAttributes_givenMultipleSearchAttrs_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clients, (clients, realm) -> {
            // Arrange
            clients.addClient(realm, "NO MATCH").setAttribute("match-1", "val");
            var c = clients.addClient(realm, "Tokyo");
            c.setAttribute("match-1", "val");
            c.setAttribute("match-2", "val");
            // Act
            var searchMap = Map.of("match-1", "val", "match-2", "val");
            Stream<ClientModel> actual = clients.searchClientsByAttributes(realm, searchMap, null, null);
            // Assert
            assertThat(actual).hasSize(1).map(ClientModel::getClientId).containsExactly("Tokyo");
        });
    }

    @TestOnServer
    public void whenAddClientScopes_givenClientScopes_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "London");
            client.setProtocol("same protocol");
            var cs1 = session.clientScopes().addClientScope(realm, "clientScope-with-same-protocol");
            cs1.setProtocol("same protocol");
            var cs2 = session.clientScopes().addClientScope(realm, "clientScope-with-unknown-protocol");
            cs2.setProtocol("unknown protocol");
            // Act
            session.clients().addClientScopes(realm, client, Set.of(cs1, cs2), false);
            // Assert
            Map<String, ClientScopeModel> actual = session.clients().getClientScopes(realm, client, false);
            assertThat(actual).hasSize(1).containsKeys("clientScope-with-same-protocol");
        });
    }

    @TestOnServer
    public void whenRemoveClientScope_givenNull_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            assertThatNoException().isThrownBy(() -> session.clients().removeClientScope(realm, null, null));
        });
    }

    @TestOnServer
    public void whenRemoveClientScope_givenNoClientScope_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            var c = session.clients().addClient(realm, "Seoul");
            assertThatNoException().isThrownBy(() -> session.clients().removeClientScope(realm, c, null));
        });
    }

    @TestOnServer
    public void whenRemoveClientScope_givenClientScope_thenClientScopeIsRemoved(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var c = session.clients().addClient(realm, "Seoul");
            var cs = session.clientScopes().addClientScope(realm, "suburb");
            c.addClientScope(cs, false);
            // Act
            session.clients().removeClientScope(realm, c, cs);
            // Assert
            assertThat(session.clients().getClientScopes(realm, c, false)).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetAllRedirectUrisOfEnabledClients_givenNoUris_thenReturnEmptyMap(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::clients, (clients, realm) -> {
            // Act
            var actual = clients.getAllRedirectUrisOfEnabledClients(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }
}
