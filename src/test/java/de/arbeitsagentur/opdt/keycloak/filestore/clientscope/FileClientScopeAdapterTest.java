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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.Map;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileClientScopeAdapterTest extends KeycloakModelTest {

    private static final String REALM_ID = "zoo";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenSetName_thenGet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            sut.setName("godzilla minus one");
            assertThat(sut.getName()).isEqualTo("godzilla_minus_one");
        });
    }

    @TestOnServer
    public void whenSetDescription_thenGet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            sut.setDescription("Large monkey");
            assertThat(sut.getDescription()).isEqualTo("Large monkey");
        });
    }

    @TestOnServer
    public void whenSetAttributes_givenUnknownKey_thenNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            assertThat(sut.getAttribute("unknown")).isNull();
        });
    }

    @TestOnServer
    public void whenSetAttributes_thenGet(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            sut.setAttribute("color", "silver");
            assertThat(sut.getAttribute("color")).isEqualTo("silver");
        });
    }

    @TestOnServer
    public void whenRemoveAttributes_givenNoAttr_thenNothingHappens(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            // Act & Assert
            assertThatNoException().isThrownBy(() -> sut.removeAttribute("color"));
        });
    }

    @TestOnServer
    public void whenRemoveAttributes_thenNull(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "gorilla");
            sut.setAttribute("color", "silver");
            // Act
            sut.removeAttribute("color");
            // Assert
            assertThat(sut.getAttribute("color")).isNull();
        });
    }

    @TestOnServer
    public void whenGetAttributes_givenNoAttributes_thenReturnEmptyStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "giraffe");
            // Act
            var actual = sut.getAttributes();
            // Assert
            assertThat(actual).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetAttributes_givenAttributes_thenReturnStream(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::clientScopes, (clients, realm) -> {
            var sut = clients.addClientScope(realm, "giraffe");
            sut.setAttribute("height", "tall");
            sut.setAttribute("color", "yellow-brown");
            // Act
            var actual = sut.getAttributes();
            // Assert
            assertThat(actual).hasSize(2).contains(Map.entry("height", "tall"), Map.entry("color", "yellow-brown"));
        });
    }
}
