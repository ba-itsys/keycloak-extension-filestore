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

package de.arbeitsagentur.opdt.keycloak.filestore.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.List;
import java.util.stream.Stream;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RoleModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileRoleProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "house";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenAddRealmRole_givenInvalidName_thenThrowException(KeycloakSession testSession) {
        nullAndEmptyStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
                assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> roles.addRealmRole(realm, invalid));
            });
        });
    }

    @TestOnServer
    public void whenAddRealmRole_givenExistingId_thenThrowException(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            // Arrange
            roles.addRealmRole(realm, "garden", "tree");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> roles.addRealmRole(realm, "garden", "bush"));
        });
    }

    @TestOnServer
    public void whenAddRealmRole_givenExistingName_thenThrowException(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            // Arrange
            roles.addRealmRole(realm, "garden");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> roles.addRealmRole(realm, "garden"));
        });
    }

    @TestOnServer
    public void whenGetClientRole_givenInvalidParam_thenReturnNull(KeycloakSession testSession) {
        nullAndEmptyStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
                // Act
                var actual = roles.getClientRole(null, invalid);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetRealmRolesStream_givenNoRoles_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::roles, (roles, realm) -> {
            // Act
            var actual = roles.getRealmRolesStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetRealmRolesStream_givenRoles_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::roles, (roles, realm) -> {
            // Arrange
            roles.addRealmRole(realm, "porch");
            roles.addRealmRole(realm, "garden");
            // Act
            var actual = roles.getRealmRolesStream(realm);
            // Assert
            assertThat(actual).hasSize(2).map(RoleModel::getName).containsExactly("garden", "porch");
        });
    }

    @TestOnServer
    public void whenGetRolesStream_givenNoRoles_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::roles, (roles, realm) -> {
            // Act
            var actual = roles.getRolesStream(realm, Stream.of(), null, null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetRolesStream_givenRoles_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            // Arrange
            roles.addRealmRole(realm, "kitchen");
            roles.addRealmRole(realm, "attic");
            // Act
            var actual = roles.getRolesStream(realm, Stream.of("kitchen"), null, null, null);
            // Assert
            assertThat(actual).hasSize(1).map(RoleModel::getId).containsExactly("kitchen");
        });
    }

    @TestOnServer
    public void whenGetRolesStream_givenSearchPattern_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            // Arrange
            roles.addRealmRole(realm, "no-match"); // no-match > exclude
            roles.addRealmRole(realm, "patty"); // partial-match > exclude
            roles.addRealmRole(realm, "abc-pattern");
            roles.addRealmRole(realm, "abc-pattern-xyz");
            roles.addRealmRole(realm, "pattern");
            roles.addRealmRole(realm, "pattern-xyz");
            // Act
            Stream<String> ids = Stream.of("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
            var actual = roles.getRolesStream(realm, ids, "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(RoleModel::getId)
                    .containsExactlyInAnyOrder("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenClient_thenNameIsSet(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "dog");
            // Act
            var actual = session.roles().addClientRole(client, "pet");
            // Assert
            assertThat(actual.getName()).isEqualTo("pet");
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenInvalidName_thenExceptionIsThrown(KeycloakSession testSession) {
        nullAndEmptyStrings().forEach(invalid -> {
            withRealm(testSession, REALM_ID, (session, realm) -> {
                // Act & Assert
                assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> session.roles().addClientRole(null, invalid));
            });
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenExistingClient_thenExceptionIsThrown(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "dog");
            session.roles().addClientRole(client, "pet");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> session.roles().addClientRole(client, "pet"));
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenExistingRole_thenExceptionIsThrown(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "dog");
            session.roles().addRealmRole(realm, "dog:pet");
            // Act & Assert
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> session.roles().addClientRole(client, "pet"));
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenIdNull_thenIdIsSet(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "jar");
            // Act
            var actual = session.roles().addClientRole(client, "chef");
            // Assert
            assertThat(actual.getId()).isEqualTo("jar:chef");
        });
    }

    @TestOnServer
    public void whenAddClientRole_givenId_thenIdIsSet(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "dog");
            // Act
            var actual = session.roles().addClientRole(client, "pet", "Pet");
            // Assert
            assertThat(actual.getId()).isEqualTo("dog:pet");
        });
    }

    @TestOnServer
    public void whenGetClientRolesStream_givenNoRoles_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "cat");
            // Act
            var actual = session.roles().getClientRolesStream(client);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetClientRolesStream_givenRoles_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "cat");
            session.roles().addClientRole(client, "pet");
            session.roles().addClientRole(client, "furry");
            // Act
            var actual = session.roles().getClientRolesStream(client);
            // Assert
            assertThat(actual).hasSize(2).map(RoleModel::getName).containsExactly("furry", "pet");
        });
    }

    @TestOnServer
    public void whenGetClientRolesStream_givenPagination_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "cat");
            session.roles().addClientRole(client, "0");
            session.roles().addClientRole(client, "1");
            session.roles().addClientRole(client, "2");
            session.roles().addClientRole(client, "3");
            // Act
            var actual = session.roles().getClientRolesStream(client, 1, 2);
            // Assert
            assertThat(actual).hasSize(2).map(RoleModel::getName).containsExactly("1", "2");
        });
    }

    @TestOnServer
    public void whenRemoveRole_givenRealmRole_thenRoleIsRemoved(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var role = session.roles().addRealmRole(realm, "living-room");
            // Act
            session.roles().removeRole(role);
            // Assert
            RoleModel removedRole = session.roles().getRoleById(realm, "living-room");
            assertThat(removedRole).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveRole_givenClientRole_thenRoleIsRemoved(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "cat");
            var role = session.roles().addClientRole(client, "alive");
            // Act
            session.roles().removeRole(role);
            // Assert
            var removedRole = session.roles().getRoleById(realm, "alive");
            assertThat(removedRole).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveRoles_givenNoRealmsRoles_thenRolesAreEmpty(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Act
            session.roles().removeRoles(realm);
            // Assert
            var actual = session.roles().getRealmRolesStream(realm);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveRoles_givenRealmRoles_thenRolesAreEmpty(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Arrange
            session.roles().addRealmRole(realm, "bed");
            session.roles().addRealmRole(realm, "pillow");
            session.roles().addRealmRole(realm, "blanket");
            // Act
            session.roles().removeRoles(realm);
            // Assert
            var actual = session.roles().getRealmRolesStream(realm);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveRoles_givenNoClientRoles_thenRolesAreEmpty(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "guest-room");
            // Act
            session.roles().removeRoles(client);
            // Assert
            var actual = client.getRolesStream();
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveRoles_givenClientRoles_thenRolesAreEmpty(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "guest-room");
            session.roles().addClientRole(client, "table");
            session.roles().addClientRole(client, "chair");
            // Act
            session.roles().removeRoles(client);
            // Assert
            var actual = client.getRolesStream();
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetRealmRole_givenInvalidName_thenReturnNull(KeycloakSession testSession) {
        nullAndEmptyStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
                // Act
                var actual = roles.getRealmRole(realm, invalid);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetRealmRole_givenNoRole_thenReturnNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            // Act
            var actual = roles.getRealmRole(realm, "abc");
            // Assert
            assertThat(actual).isNull();
        });
    }

    @TestOnServer
    public void whenGetRoleById_givenInvalidParam_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
                // Act
                var actual = roles.getRoleById(realm, invalid);
                // Assert
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetRoleById_givenExistingId_thenReturnRole(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            roles.addRealmRole(realm, "freezer");
            // Act
            var actual = roles.getRoleById(realm, "freezer");
            // Assert
            assertThat(actual.getName()).isEqualTo("freezer");
        });
    }

    @TestOnServer
    public void whenSearchForRolesStream_givenInvalidSearch_thenReturnEmptyStream(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withCleanRealmAndProvider(testSession, KeycloakSession::roles, (roles, realm) -> {
                // Act
                var actual = roles.searchForRolesStream(realm, invalid, null, null);
                // Assert
                assertThat(actual).isEmpty();
            });
        });
    }

    @TestOnServer
    public void whenSearchForRolesStream_givenSearchString_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::roles, (roles, realm) -> {
            roles.addRealmRole(realm, "patty"); // partial match -> exclude
            roles.addRealmRole(realm, "no-match"); // no match -> exclude
            roles.addRealmRole(realm, "abc-pattern");
            roles.addRealmRole(realm, "abc-pattern-xyz");
            roles.addRealmRole(realm, "pattern");
            roles.addRealmRole(realm, "pattern-xyz");
            // Act
            var actual = roles.searchForRolesStream(realm, "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(RoleModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenSearchForClientRolesStream_givenSearchString_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var client = session.clients().addClient(realm, "bathtub");

            session.roles().addClientRole(client, "patty"); // partial match -> exclude
            session.roles().addClientRole(client, "no-match"); // no match -> exclude
            session.roles().addClientRole(client, "abc-pattern");
            session.roles().addClientRole(client, "abc-pattern-xyz");
            session.roles().addClientRole(client, "pattern");
            session.roles().addClientRole(client, "pattern-xyz");
            // Act
            var actual = session.roles().searchForClientRolesStream(client, "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(RoleModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenSearchForClientRolesStream_givenSearchNull_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Act
            var actual = session.roles().searchForClientRolesStream(realm, Stream.of(), null, null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchForClientRolesStream_givenIds_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var ids = List.of(
                    "bathtub:patty", // partial match -> exclude
                    "bathtub:no-match", // no match -> exclude
                    "bathtub:abc-pattern",
                    "bathtub:abc-pattern-xyz",
                    "bathtub:pattern",
                    "bathtub:pattern-xyz");

            var client = session.clients().addClient(realm, "bathtub");

            ids.forEach(id -> session.roles().addClientRole(client, id.split(":")[1]));
            // Act
            var actual = session.roles().searchForClientRolesStream(realm, ids.stream(), "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(RoleModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenSearchForClientRolesStream_givenSearchNullAndExcludedIds_thenReturnEmptyStream(
            KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Act
            var actual = session.roles().searchForClientRolesStream(realm, null, Stream.of(), null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchForClientRolesStream_givenExcludedIds_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            var ids = List.of(
                    "patty", // partial match -> exclude
                    "no-match", // no match -> exclude
                    "abc-pattern",
                    "abc-pattern-xyz",
                    "pattern",
                    "pattern-xyz");

            var client = session.clients().addClient(realm, "bathtub");

            ids.forEach(id -> session.roles().addClientRole(client, id));
            // Act
            Stream<String> excludedIds = Stream.of("bathtub:abc-pattern", "bathtub:abc-pattern-xyz");
            var actual = session.roles().searchForClientRolesStream(realm, "pattern", excludedIds, null, null);
            // Assert
            assertThat(actual).hasSize(2).map(RoleModel::getName).containsExactly("pattern", "pattern-xyz");
        });
    }
}
