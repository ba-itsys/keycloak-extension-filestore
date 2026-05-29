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

package de.arbeitsagentur.opdt.keycloak.filestore.group;

import static org.assertj.core.api.Assertions.assertThat;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileGroupProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "river";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenGetGroupById_givenInvalidParam_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
                var actual = groups.getGroupById(realm, invalid);
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetGroupById_givenExistingGroup_thenReturnNotNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            groups.createGroup(realm, "Nile");
            var actual = groups.getGroupById(realm, "Nile");
            assertThat(actual.getId()).isEqualTo("Nile");
        });
    }

    @TestOnServer
    public void whenCreateGroup_givenExplicitId_thenNameIsUsedAsReadableIdentifier(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            var actual = groups.createGroup(realm, "generated-id", "Readable Group");

            assertThat(actual.getId()).isEqualTo("Readable Group");
            assertThat(groups.getGroupById(realm, "Readable Group")).isNotNull();
            assertThat(groups.getGroupById(realm, "generated-id")).isNull();
        });
    }

    @TestOnServer
    public void whenGetGroupByName_givenInvalidParam_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
                var actual = groups.getGroupByName(realm, null, invalid);
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetGroupByName_givenExistingGroup_thenReturnNotNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            groups.createGroup(realm, "Amazon");
            var actual = groups.getGroupByName(realm, null, "Amazon");
            assertThat(actual.getId()).isEqualTo("Amazon");
        });
    }

    @TestOnServer
    public void whenGetGroupByName_givenParentGroup_thenReturnNotNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var parent = groups.createGroup(realm, "Parent");
            groups.createGroup(realm, "Child").setParent(parent);
            // Act
            var actual = groups.getGroupByName(realm, parent, "Child");
            // Assert
            assertThat(actual.getId()).isEqualTo("Child");
        });
    }

    @TestOnServer
    public void whenGetGroupsStream_givenNoGroups_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Act
            var actual = groups.getGroupsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetGroupsStream_givenGroups_thenReturnStream(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "Yukon");
            groups.createGroup(realm, "Rio-Grande");
            // Act
            var actual = groups.getGroupsStream(realm);
            // Assert
            assertThat(actual).hasSize(2).map(GroupModel::getName).containsExactly("Rio-Grande", "Yukon");
        });
    }

    @TestOnServer
    public void whenGetGroupsStreamWithIds_givenNoGroups_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Act
            var actual = groups.getGroupsStream(realm, Stream.of());
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetGroupsStreamWithIds_givenUnknownGroups_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Act
            var actual = groups.getGroupsStream(realm, Stream.of("unknown"));
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetGroupsStreamWithIds_givenGroups_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "Yukon");
            groups.createGroup(realm, "Rio-Grande");
            // Act
            var actual = groups.getGroupsStream(realm, Stream.of("Yukon"));
            // Assert
            assertThat(actual).hasSize(1).map(GroupModel::getName).containsExactly("Yukon");
        });
    }

    @TestOnServer
    public void whenGetGroupsStreamWithIds_givenSearchPattern_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "patty"); // partial -> exclude
            groups.createGroup(realm, "no-match"); // no match -> exclude
            groups.createGroup(realm, "abc-pattern");
            groups.createGroup(realm, "abc-pattern-xyz");
            groups.createGroup(realm, "pattern");
            groups.createGroup(realm, "pattern-xyz");
            // Act
            var ids = Stream.of("patty", "no-match", "pattern", "pattern-xyz", "abc-pattern", "abc-pattern-xyz");
            var actual = groups.getGroupsStream(realm, ids, "pattern", null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(GroupModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenGetGroupsCount_givenNoGroup_thenReturnZero(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Act
            Long actual = groups.getGroupsCount(realm, false);
            // Assert
            assertThat(actual).isZero();
        });
    }

    @TestOnServer
    public void whenGetGroupsCount_givenGroups_thenReturnCount(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "Ob");
            groups.createGroup(realm, "Niger");
            // Act
            Long actual = groups.getGroupsCount(realm, false);
            // Assert
            assertThat(actual).isEqualTo(2);
        });
    }

    @TestOnServer
    public void whenGetGroupsCount_givenTopGroups_thenReturnTopCount(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var parent = groups.createGroup(realm, "Ob");
            groups.createGroup(realm, "Niger", parent);
            // Act
            Long actual = groups.getGroupsCount(realm, true);
            // Assert
            assertThat(actual).isEqualTo(1);
        });
    }

    @TestOnServer
    public void whenGetGroupsCountByNameContaining_givenInvalidParam_thenReturnZero(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
                // Act
                Long actual = groups.getGroupsCountByNameContaining(realm, invalid);
                // Assert
                assertThat(actual).isZero();
            });
        });
    }

    @TestOnServer
    public void whenGetGroupsCountByNameContaining_givenGroups_thenReturnCount(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "patty"); // partial -> no match
            groups.createGroup(realm, "abc-pattern");
            groups.createGroup(realm, "abc-pattern-xyz");
            groups.createGroup(realm, "pattern");
            groups.createGroup(realm, "pattern-xyz");
            // Act
            Long actual = groups.getGroupsCountByNameContaining(realm, "pattern");
            // Assert
            assertThat(actual).isEqualTo(4);
        });
    }

    @TestOnServer
    public void whenGetGroupsByRoleStream_givenNoGroups_thenReturnEmptyStreams(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Arrange
            var role = session.roles().addRealmRole(realm, "unassigned");
            // Act
            var actual = session.groups().getGroupsByRoleStream(realm, role, null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetGroupsByRoleStream_givenGroups_thenReturnStreams(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Arrange
            var role = session.roles().addRealmRole(realm, "Fisherman");
            session.groups().createGroup(realm, "Orinoco").grantRole(role);
            // Act
            var actual = session.groups().getGroupsByRoleStream(realm, role, null, null);
            // Assert
            assertThat(actual).hasSize(1).map(GroupModel::getName).containsExactly("Orinoco");
        });
    }

    @TestOnServer
    public void whenGetTopLevelGroupsStream_givenNoGroups_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Act
            var actual = session.groups().getTopLevelGroupsStream(realm);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetTopLevelGroupsStream_giveParentChildGroups_thenReturnStream(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Arrange
            session.groups().createGroup(realm, "Tigris");
            var parent = session.groups().createGroup(realm, "Nile");
            session.groups().createGroup(realm, "Blue-Nile", parent);
            // Act
            var actual = session.groups().getTopLevelGroupsStream(realm);
            // Assert
            assertThat(actual).hasSize(2).map(GroupModel::getName).containsExactly("Nile", "Tigris");
        });
    }

    @TestOnServer
    public void whenGetTopLevelGroupsStream_givenPagination_thenReturnStream(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Arrange
            session.groups().createGroup(realm, "Colorado");
            session.groups().createGroup(realm, "Nile"); // start - firstResult = 1
            session.groups().createGroup(realm, "Tigris"); // end - maxResults = 2
            session.groups().createGroup(realm, "Yangtze");
            // Act
            var actual = session.groups().getTopLevelGroupsStream(realm, 1, 2);
            // Assert
            assertThat(actual).hasSize(2).map(GroupModel::getName).containsExactly("Nile", "Tigris");
        });
    }

    @TestOnServer
    public void whenGetTopLevelGroupsStream_givenSearchPattern_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            session.groups().createGroup(realm, "patty");
            session.groups().createGroup(realm, "abc-pattern");
            session.groups().createGroup(realm, "abc-pattern-xyz");
            session.groups().createGroup(realm, "pattern");
            session.groups().createGroup(realm, "pattern-xyz");
            // Act
            var actual = session.groups().getTopLevelGroupsStream(realm, "pattern", false, null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(GroupModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenGetTopLevelGroupsStream_givenExactSearch_thenReturnStream(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // Arrange
            session.groups().createGroup(realm, "patty");
            session.groups().createGroup(realm, "abc-pattern");
            session.groups().createGroup(realm, "abc-pattern-xyz");
            session.groups().createGroup(realm, "pattern");
            session.groups().createGroup(realm, "pattern-xyz");
            // Act
            var actual = session.groups().getTopLevelGroupsStream(realm, "pattern", false, null, null);
            // Assert
            assertThat(actual)
                    .hasSize(4)
                    .map(GroupModel::getName)
                    .containsExactly("abc-pattern", "abc-pattern-xyz", "pattern", "pattern-xyz");
        });
    }

    @TestOnServer
    public void whenSearchGroupsByAttributes_givenNoGroups_thenReturnEmptyStream(KeycloakSession testSession) {
        withCleanRealm(testSession, (session, realm) -> {
            // Act
            var actual = session.groups().searchGroupsByAttributes(realm, Map.of(), null, null);
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenSearchGroupsByAttributes_givenGroups_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            groups.createGroup(realm, "Rhine").setAttribute("key", List.of("value"));
            groups.createGroup(realm, "Nile").setAttribute("key", List.of("value"));
            groups.createGroup(realm, "Amazon").setAttribute("no-match", List.of("value"));
            // Act
            var actual = groups.searchGroupsByAttributes(realm, Map.of("key", "value"), null, null);
            // Assert
            assertThat(actual).hasSize(2).map(GroupModel::getName).containsExactly("Nile", "Rhine");
        });
    }

    @TestOnServer
    public void whenRemoveGroup_givenNull_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Act
            var actual = groups.removeGroup(realm, null);
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenRemoveGroup_givenGroup_thenReturnTrue(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var group = groups.createGroup(realm, "Ganges");
            // Act
            var actual = groups.removeGroup(realm, group);
            // Assert
            assertThat(actual).isTrue();
            assertThat(groups.getGroupByName(realm, null, "Ganges")).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveGroup_givenAlreadyRemovedGroup_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var group = groups.createGroup(realm, "Ganges");
            groups.removeGroup(realm, group); // remove intentionally
            // Act
            var actual = groups.removeGroup(realm, group);
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenAddTopLevelGroup_givenChild_thenChildIsGrownUp(KeycloakSession testSession) {
        withCleanRealmAndProvider(testSession, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var parent = groups.createGroup(realm, "Parent");
            var child = groups.createGroup(realm, "Child", parent);
            assertThat(groups.getGroupsCount(realm, /* onlyTopGroups */ true)).isEqualTo(1);
            // Act
            groups.addTopLevelGroup(realm, child);
            // Assert
            assertThat(child.getParentId()).isNull();
            assertThat(groups.getGroupsCount(realm, /* onlyTopGroups */ true)).isEqualTo(2);
        });
    }

    @TestOnServer
    public void whenMoveGroup_givenGroups_thenGroupIsMoved(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::groups, (groups, realm) -> {
            // Arrange
            var previousParent = groups.createGroup(realm, "Parent1");
            var nextParent = groups.createGroup(realm, "Parent2");
            var fosterChild = groups.createGroup(realm, "Child", previousParent);
            // Act
            groups.moveGroup(realm, fosterChild, nextParent);
            // Assert
            assertThat(fosterChild.getParentId()).isEqualTo(nextParent.getId());
        });
    }
}
