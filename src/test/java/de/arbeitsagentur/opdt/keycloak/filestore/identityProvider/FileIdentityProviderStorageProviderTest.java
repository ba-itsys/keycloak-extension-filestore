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

package de.arbeitsagentur.opdt.keycloak.filestore.identityProvider;

import static org.assertj.core.api.Assertions.assertThat;

import de.arbeitsagentur.opdt.keycloak.filestore.KeycloakModelTest;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import java.util.Map;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.keycloak.models.*;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;

@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class FileIdentityProviderStorageProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "mountain";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    private static final String LOGIN_PROVIDER_ID = "oidc";

    @TestOnServer
    public void whenCreateIdentityProvider_givenValidData_thenIdpCanBeRead(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setEnabled(true);
            model.setProviderId("bundid");
            idps.create(model);

            assertThat(idps.getByAlias("meinIdp"))
                    .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields(
                                    "internalId",
                                    "addReadTokenRoleOnCreate",
                                    "authenticateByDefault",
                                    "linkOnly",
                                    "storeToken",
                                    "trustEmail")
                            .build())
                    .isEqualTo(model);
        });
    }

    @TestOnServer
    public void whenGetById_givenExistingIdp_thenIdpCanBeRead(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setEnabled(true);
            model.setProviderId("bundid");
            IdentityProviderModel createdModel = idps.create(model);

            assertThat(idps.getById(createdModel.getInternalId()).getAlias()).isEqualTo("meinIdp");
        });
    }

    @TestOnServer
    public void whenUpdateIdentityProvider_givenChangedData_thenIdpIsUpdated(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setDisplayName("original");
            model.setEnabled(true);
            model.setHideOnLogin(false);
            model.setProviderId(LOGIN_PROVIDER_ID);
            IdentityProviderModel createdModel = idps.create(model);

            createdModel.setDisplayName("updated");
            createdModel.setEnabled(false);
            createdModel.setHideOnLogin(true);
            createdModel.setConfig(Map.of("key", "value"));
            idps.update(createdModel);

            IdentityProviderModel updatedModel = idps.getByAlias("meinIdp");
            assertThat(updatedModel.getDisplayName()).isEqualTo("updated");
            assertThat(updatedModel.isEnabled()).isFalse();
            assertThat(updatedModel.isHideOnLogin()).isTrue();
            assertThat(updatedModel.getConfig()).containsEntry("key", "value");
        });
    }

    @TestOnServer
    public void whenCount_givenIdpsExist_thenReturnsNumberOfIdps(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("bundid");
            idps.create(model2);

            assertThat(idps.count()).isEqualTo(2);
        });
    }

    @TestOnServer
    public void whenRemoveIdentityProvider_idpExists_thenIdpIsDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setEnabled(true);
            model.setProviderId("bundid");
            idps.create(model);

            idps.remove("meinIdp");

            assertThat(idps.getByAlias("meinIdp")).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveAllIdps_idpsExist_thenAllIdpsAreDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("bundid");
            idps.create(model2);

            idps.removeAll();

            assertThat(idps.getByAlias("meinIdp")).isNull();
            assertThat(idps.getByAlias("meinIdp2")).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveIdentityProvider_idpDoesNotExist_thenNoExceptionIsThrown(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            idps.remove("meinIdp");

            assertThat(idps.getByAlias("meinIdp")).isNull();
        });
    }

    @TestOnServer
    public void whenGetForLogin_fetchModeRealm_thenReturnsAllRealmIdps(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("meinIdp3");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId(LOGIN_PROVIDER_ID);
            orgIdp.setOrganizationId("myorg");
            idps.create(orgIdp);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.REALM_ONLY, null)
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp", "meinIdp2");
        });
    }

    @TestOnServer
    public void whenGetForLogin_fetchModeOrg_thenReturnsAllOrgIdps(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("meinIdp3");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId(LOGIN_PROVIDER_ID);
            orgIdp.setOrganizationId("myorg");
            idps.create(orgIdp);

            IdentityProviderModel orgIdp2 = new IdentityProviderModel();
            orgIdp2.setAlias("meinIdp4");
            orgIdp2.setEnabled(true);
            orgIdp2.setProviderId(LOGIN_PROVIDER_ID);
            orgIdp2.setOrganizationId("myorg2");
            idps.create(orgIdp2);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.ORG_ONLY, "myorg")
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp3");
        });
    }

    @TestOnServer
    public void whenGetForLogin_fetchModeAll_thenReturnsAllIdps(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("meinIdp3");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId(LOGIN_PROVIDER_ID);
            orgIdp.setOrganizationId("myorg");
            idps.create(orgIdp);

            IdentityProviderModel orgIdp2 = new IdentityProviderModel();
            orgIdp2.setAlias("meinIdp4");
            orgIdp2.setEnabled(true);
            orgIdp2.setProviderId(LOGIN_PROVIDER_ID);
            orgIdp2.setOrganizationId("myorg2");
            idps.create(orgIdp2);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.ALL, "myorg2")
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp", "meinIdp2", "meinIdp4");
        });
    }

    @TestOnServer
    public void whenGetForLogin_givenUnknownProviderFactory_thenIdpIsHidden(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setEnabled(true);
            model.setProviderId("unknown-provider");
            idps.create(model);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.REALM_ONLY, null))
                    .isEmpty();
        });
    }

    @TestOnServer
    public void whenGetForLogin_givenHiddenIdp_thenIdpIsHidden(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel visibleModel = new IdentityProviderModel();
            visibleModel.setAlias("visibleIdp");
            visibleModel.setEnabled(true);
            visibleModel.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(visibleModel);

            IdentityProviderModel hiddenModel = new IdentityProviderModel();
            hiddenModel.setAlias("hiddenIdp");
            hiddenModel.setEnabled(true);
            hiddenModel.setHideOnLogin(true);
            hiddenModel.setProviderId(LOGIN_PROVIDER_ID);
            idps.create(hiddenModel);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.REALM_ONLY, null)
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("visibleIdp");
        });
    }

    @TestOnServer
    public void whenGetByFlow_flowIdExists_thenReturnsMatchingIdps(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            model1.setPostBrokerLoginFlowId("flow1");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("muk");
            model2.setFirstBrokerLoginFlowId("flow1");
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("meinIdp3");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId("orgi");
            orgIdp.setOrganizationId("myorg");
            idps.create(orgIdp);

            IdentityProviderModel orgIdp2 = new IdentityProviderModel();
            orgIdp2.setAlias("meinIdp4");
            orgIdp2.setEnabled(true);
            orgIdp2.setProviderId("orgi");
            orgIdp2.setOrganizationId("myorg2");
            orgIdp2.setFirstBrokerLoginFlowId("flow2");
            idps.create(orgIdp2);

            assertThat(idps.getByFlow("flow1", null, null, null)).containsExactly("meinIdp", "meinIdp2");
        });
    }

    @TestOnServer
    public void whenGetByFlow_givenSearchTerm_thenReturnsMatchingIdps(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            IdentityProviderStorageProvider idps = session.identityProviders();
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            model1.setPostBrokerLoginFlowId("flow1");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("muk");
            model2.setFirstBrokerLoginFlowId("flow1");
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("ichHeißeKomisch");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId("orgi");
            orgIdp.setOrganizationId("myorg");
            orgIdp.setFirstBrokerLoginFlowId("flow1");
            idps.create(orgIdp);

            IdentityProviderModel orgIdp2 = new IdentityProviderModel();
            orgIdp2.setAlias("meinIdp4");
            orgIdp2.setEnabled(true);
            orgIdp2.setProviderId("orgi");
            orgIdp2.setOrganizationId("myorg2");
            orgIdp2.setFirstBrokerLoginFlowId("flow1");
            idps.create(orgIdp2);

            assertThat(idps.getByFlow("flow1", "*Idp*", null, null)).containsExactly("meinIdp", "meinIdp2", "meinIdp4");
        });
    }

    @TestOnServer
    public void whenCreateMapper_givenValidData_thenMapperCanBeRead(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            idps.createMapper(model);

            assertThat(idps.getMapperByName("meinIdp1", "meinMapper")).isEqualTo(model);
            assertThat(idps.getMapperById("blubb")).isEqualTo(model);
        });
    }

    @TestOnServer
    public void whenUpdateMapper_givenExistingMapper_thenMapperIsUpdated(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            model.setConfig(Map.of("key", "old"));
            idps.createMapper(model);

            model.setName("meinMapperUpdated");
            model.setIdentityProviderMapper("mapperId2");
            model.setConfig(Map.of("key", "new"));
            idps.updateMapper(model);

            IdentityProviderMapperModel updatedModel = idps.getMapperById("blubb");
            assertThat(updatedModel.getName()).isEqualTo("meinMapperUpdated");
            assertThat(updatedModel.getIdentityProviderMapper()).isEqualTo("mapperId2");
            assertThat(updatedModel.getConfig()).containsEntry("key", "new");
        });
    }

    @TestOnServer
    public void whenRemoveMapper_givenExistingMapper_thenMapperIsDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            idps.createMapper(model);

            idps.removeMapper(model);

            assertThat(idps.getMapperById("blubb")).isNull();
        });
    }

    @TestOnServer
    public void whenRemoveAllMappers_givenExistingMappers_thenAllMappersAreDeleted(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model1 = new IdentityProviderMapperModel();
            model1.setId("blubb");
            model1.setName("meinMapper");
            model1.setIdentityProviderAlias("meinIdp1");
            model1.setIdentityProviderMapper("mapperId1");
            idps.createMapper(model1);

            IdentityProviderMapperModel model2 = new IdentityProviderMapperModel();
            model2.setId("blubb2");
            model2.setName("meinMapper2");
            model2.setIdentityProviderAlias("meinIdp2");
            model2.setIdentityProviderMapper("mapperId2");
            idps.createMapper(model2);

            idps.removeAllMappers();

            assertThat(idps.getMappersStream()).isEmpty();
        });
    }

    @TestOnServer
    public void whenGetMapperByAlias_givenMultipleMappers_thenMatchingMappersAreReturned(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            idps.createMapper(model);

            IdentityProviderMapperModel model2 = new IdentityProviderMapperModel();
            model2.setId("blubb2");
            model2.setName("meinMapper2");
            model2.setIdentityProviderAlias("meinIdp1");
            model2.setIdentityProviderMapper("mapperId2");
            idps.createMapper(model2);

            IdentityProviderMapperModel model3 = new IdentityProviderMapperModel();
            model3.setId("blubb3");
            model3.setName("meinMapper3");
            model3.setIdentityProviderAlias("meinIdp2");
            model3.setIdentityProviderMapper("mapperId3");
            idps.createMapper(model3);

            assertThat(idps.getMappersByAliasStream("meinIdp1").map(IdentityProviderMapperModel::getId))
                    .containsExactly("blubb", "blubb2");
        });
    }

    @TestOnServer
    public void whenGetMappers_givenMultipleMappers_thenAllMappersAreReturned(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            idps.createMapper(model);

            IdentityProviderMapperModel model2 = new IdentityProviderMapperModel();
            model2.setId("blubb2");
            model2.setName("meinMapper2");
            model2.setIdentityProviderAlias("meinIdp1");
            model2.setIdentityProviderMapper("mapperId2");
            idps.createMapper(model2);

            IdentityProviderMapperModel model3 = new IdentityProviderMapperModel();
            model3.setId("blubb3");
            model3.setName("meinMapper3");
            model3.setIdentityProviderAlias("meinIdp2");
            model3.setIdentityProviderMapper("mapperId3");
            idps.createMapper(model3);

            assertThat(idps.getMappersStream().map(IdentityProviderMapperModel::getId))
                    .containsExactly("blubb", "blubb2", "blubb3");
        });
    }

    @TestOnServer
    public void whenGetMappers_givenSearchOptions_thenAllMatchingMappersAreReturned(KeycloakSession testSession) {
        withRealmAndProvider(testSession, REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderMapperModel model = new IdentityProviderMapperModel();
            model.setId("blubb");
            model.setName("meinMapper");
            model.setIdentityProviderAlias("meinIdp1");
            model.setIdentityProviderMapper("mapperId1");
            model.setConfig(Map.of("x", "y"));
            idps.createMapper(model);

            IdentityProviderMapperModel model2 = new IdentityProviderMapperModel();
            model2.setId("blubb2");
            model2.setName("meinMapper2");
            model2.setIdentityProviderAlias("meinIdp1");
            model2.setIdentityProviderMapper("mapperId2");
            model2.setConfig(Map.of("a", "b", "x", "y"));
            idps.createMapper(model2);

            IdentityProviderMapperModel model3 = new IdentityProviderMapperModel();
            model3.setId("blubb3");
            model3.setName("meinMapper3");
            model3.setIdentityProviderAlias("meinIdp2");
            model3.setIdentityProviderMapper("mapperId3");
            idps.createMapper(model3);

            assertThat(idps.getMappersStream(Map.of("x", "y", "a", "b"), null, null)
                            .map(IdentityProviderMapperModel::getId))
                    .containsExactly("blubb2");
        });
    }
}
