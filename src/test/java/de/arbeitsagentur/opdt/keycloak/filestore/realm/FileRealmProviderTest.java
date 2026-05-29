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

package de.arbeitsagentur.opdt.keycloak.filestore.realm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.MapAssert.assertThatMap;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.Map;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileRealmProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "mountain";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenCreateRealm_givenInvalidName_thenExceptionIsThrown(KeycloakSession testSession) {
        nullAndEmptyStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
                assertThatExceptionOfType(IllegalArgumentException.class)
                        .isThrownBy(() -> realms.createRealm(REALM_ID, invalid));
            });
        });
    }

    @TestOnServer
    public void whenCreateRealm_givenExistingId_thenExceptionIsThrown(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> realms.createRealm(REALM_ID, REALM_ID));
        });
    }

    @TestOnServer
    public void whenCreateRealm_givenExistingName_thenExceptionIsThrown(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            assertThatExceptionOfType(ModelDuplicateException.class)
                    .isThrownBy(() -> realms.createRealm(null, REALM_ID));
        });
    }

    @TestOnServer
    public void whenCreateRealm_givenNullId_thenSetNameAsId(KeycloakSession testSession) {
        inCommittedTransaction(testSession, session -> {
            var actual = session.realms().createRealm(null, "K2");
            assertThat(actual.getId()).isEqualTo("K2");

            // Teardown
            session.realms().removeRealm("K2");
        });
    }

    @TestOnServer
    public void whenGetRealm_givenNull_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
                var actual = realms.getRealm(invalid);
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetRealmByName_givenNull_thenReturnNull(KeycloakSession testSession) {
        nullEmptyAndUnknownStrings().forEach(invalid -> {
            withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
                var actual = realms.getRealmByName(invalid);
                assertThat(actual).isNull();
            });
        });
    }

    @TestOnServer
    public void whenGetRealmByName_givenExistingRealm_thenReturnRealm(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            var actual = realms.getRealmByName(REALM_ID);
            assertThat(actual.getId()).isEqualTo(REALM_ID);
        });
    }

    @TestOnServer
    public void whenGetRealmsWithProviderTypeStream_givenObjectClass_thenReturnEmptyStream(
            KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            var actual = realms.getRealmsWithProviderTypeStream(Object.class);
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenRemoveRealm_givenNoRealm_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            var actual = realms.removeRealm("unknown");
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenRemoveExpiredClientInitialAccess_givenMultipleClients_thenExpiredClientsAreRemoved(
            KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Arrange
            realms.createClientInitialAccessModel(realm, -5000, 0); // expired
            realms.createClientInitialAccessModel(realm, -1000, 0); // expired
            realms.createClientInitialAccessModel(realm, -1, 0); // expired
            realms.createClientInitialAccessModel(realm, 1000, 0);
            assertThat(realm.getClientInitialAccesses()).hasSize(4);
            // Act
            realms.removeExpiredClientInitialAccess();
            // Assert
            assertThat(realm.getClientInitialAccesses()).hasSize(1);
        });
    }

    @TestOnServer
    public void whenSaveLocalizationText_givenNull_thenNoLocalizationIsSet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            realms.saveLocalizationText(realm, null, null, null);
            // Assert
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de")).isEmpty();
        });
    }

    @TestOnServer
    public void whenSaveLocalizationText_givenValues_thenLocalizationIsSet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            realms.saveLocalizationText(realm, "de-de", "TREE", "Baum");
            // Assert
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de")).contains(Map.entry("TREE", "Baum"));
        });
    }

    @TestOnServer
    public void whenSaveLocalizationTexts_givenEmptyMap_thenNoLocalizationsAreSet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            realms.saveLocalizationTexts(realm, "de-de", Map.of());
            // Assert
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de")).isEmpty();
        });
    }

    @TestOnServer
    public void whenSaveLocalizationTexts_givenMultipleValues_thenLocalizationsAreSet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            Map<String, String> localizationTexts = Map.of(
                    "TREE", "Baum",
                    "HOUSE", "Haus",
                    "LAKE", "See");
            realms.saveLocalizationTexts(realm, "de-de", localizationTexts);
            // Assert
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de"))
                    .contains(Map.entry("TREE", "Baum"), Map.entry("HOUSE", "Haus"), Map.entry("LAKE", "See"));
        });
    }

    @TestOnServer
    public void whenUpdateLocalizationText_givenInvalidValue_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.updateLocalizationText(realm, null, null, null);
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenUpdateLocalizationText_givenUnknownKey_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.updateLocalizationText(realm, "de-de", "unknown", "unbekannt");
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenUpdateLocalizationText_givenValue_thenLocalizationIsUpdated(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Arrange
            realms.saveLocalizationText(realm, "de-de", "COCK", "Gockel");
            // Act
            var actual = realms.updateLocalizationText(realm, "de-de", "COCK", "Hahn");
            // Assert
            assertThat(actual).isTrue();
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de")).contains(Map.entry("COCK", "Hahn"));
        });
    }

    @TestOnServer
    public void whenDeleteLocalizationTextsByLocale_givenValue_thenLocalizationIsDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Arrange
            realms.saveLocalizationText(realm, "de-de", "TABLE", "Tisch");
            realms.saveLocalizationText(realm, "de-de", "CHAIR", "Stuhl");
            // Act
            var actual = realms.deleteLocalizationTextsByLocale(realm, "de-de");
            // Assert
            assertThat(actual).isTrue();
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de")).isEmpty();
        });
    }

    @TestOnServer
    public void whenDeleteLocalizationText_givenNullValue_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.deleteLocalizationText(realm, null, null);
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenDeleteLocalizationText_givenUnknownKey_thenReturnFalse(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.deleteLocalizationText(realm, "de-de", "unknown");
            // Assert
            assertThat(actual).isFalse();
        });
    }

    @TestOnServer
    public void whenDeleteLocalizationText_givenValue_thenLocalizationIsDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Arrange
            realms.saveLocalizationText(realm, "de-de", "TABLE", "Tisch");
            realms.saveLocalizationText(realm, "de-de", "CHAIR", "Stuhl");
            // Act
            var actual = realms.deleteLocalizationText(realm, "de-de", "TABLE");
            // Assert
            assertThat(actual).isTrue();
            assertThatMap(realm.getRealmLocalizationTextsByLocale("de-de"))
                    .hasSize(1)
                    .contains(Map.entry("CHAIR", "Stuhl"));
        });
    }

    @TestOnServer
    public void whenGetLocalizationText_givenNull_thenReturnNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.getLocalizationTextsById(realm, null, null);
            // Assert
            assertThat(actual).isNull();
        });
    }

    @TestOnServer
    public void whenGetLocalizationText_givenUnknownKey_thenReturnNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Act
            var actual = realms.getLocalizationTextsById(realm, "en-us", "unknown");
            // Assert
            assertThat(actual).isNull();
        });
    }

    @TestOnServer
    public void whenGetLocalizationText_givenValue_thenReturnValue(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::realms, (realms, realm) -> {
            // Arrange
            realms.saveLocalizationText(realm, "de-de", "TABLE", "Tisch");
            realms.saveLocalizationText(realm, "en-us", "CHAIR", "chair");
            // Act
            var actual = realms.getLocalizationTextsById(realm, "de-de", "TABLE");
            // Assert
            assertThat(actual).isEqualTo("Tisch");
        });
    }
}
