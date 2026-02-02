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
import java.util.Map;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.keycloak.models.*;

class FileIdentityProviderStorageProviderTest extends KeycloakModelTest {

    private static final String REALM_ID = "mountain";

    @Override
    protected void createEnvironment(KeycloakSession s) {
        s.realms().createRealm(REALM_ID);
    }

    @Override
    protected void cleanEnvironment(KeycloakSession s) {
        s.realms().removeRealm(REALM_ID);
    }

    @Test
    void whenCreateIdentityProvider_givenValidData_thenIdpCanBeRead() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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
                                    "trustEmail",
                                    "hideOnLogin")
                            .build())
                    .isEqualTo(model);
        });
    }

    @Test
    void whenRemoveIdentityProvider_idpExists_thenIdpIsDeleted() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("meinIdp");
            model.setEnabled(true);
            model.setProviderId("bundid");
            idps.create(model);

            idps.remove("meinIdp");

            assertThat(idps.getByAlias("meinIdp")).isNull();
        });
    }

    @Test
    void whenRemoveAllIdps_idpsExist_thenAllIdpsAreDeleted() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenRemoveIdentityProvider_idpDoesNotExist_thenNoExceptionIsThrown() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            idps.remove("meinIdp");

            assertThat(idps.getByAlias("meinIdp")).isNull();
        });
    }

    @Test
    void whenGetForLogin_fetchModeRealm_thenReturnsAllRealmIdps() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("muk");
            idps.create(model2);

            IdentityProviderModel orgIdp = new IdentityProviderModel();
            orgIdp.setAlias("meinIdp3");
            orgIdp.setEnabled(true);
            orgIdp.setProviderId("orgi");
            orgIdp.setOrganizationId("myorg");
            idps.create(orgIdp);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.REALM_ONLY, null)
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp", "meinIdp2");
        });
    }

    @Test
    void whenGetForLogin_fetchModeOrg_thenReturnsAllOrgIdps() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("muk");
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
            idps.create(orgIdp2);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.ORG_ONLY, "myorg")
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp3");
        });
    }

    @Test
    void whenGetForLogin_fetchModeAll_thenReturnsAllIdps() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
            IdentityProviderModel model1 = new IdentityProviderModel();
            model1.setAlias("meinIdp");
            model1.setEnabled(true);
            model1.setProviderId("bundid");
            idps.create(model1);

            IdentityProviderModel model2 = new IdentityProviderModel();
            model2.setAlias("meinIdp2");
            model2.setEnabled(true);
            model2.setProviderId("muk");
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
            idps.create(orgIdp2);

            assertThat(idps.getForLogin(IdentityProviderStorageProvider.FetchMode.ALL, "myorg2")
                            .map(IdentityProviderModel::getAlias))
                    .containsExactly("meinIdp", "meinIdp2", "meinIdp4");
        });
    }

    @Test
    void whenGetByFlow_flowIdExists_thenReturnsMatchingIdps() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenGetByFlow_givenSearchTerm_thenReturnsMatchingIdps() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenCreateMapper_givenValidData_thenMapperCanBeRead() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenGetMapperByAlias_givenMultipleMappers_thenMatchingMappersAreReturned() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenGetMappers_givenMultipleMappers_thenAllMappersAreReturned() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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

    @Test
    void whenGetMappers_givenSearchOptions_thenAllMatchingMappersAreReturned() {
        withRealmAndProvider(REALM_ID, KeycloakSession::identityProviders, (idps, realm) -> {
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
