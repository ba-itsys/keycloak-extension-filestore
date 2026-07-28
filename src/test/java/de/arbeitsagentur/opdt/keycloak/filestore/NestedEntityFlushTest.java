/*
 * Copyright 2026. IT-Systemhaus der Bundesagentur fuer Arbeit
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

import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.config.FileStoreKeycloakServerConfig;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmEntity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Map;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * In-place updates of nested realm entities (components, flows, executions, configs, identity
 * providers, ...) do not go through a {@link FileRealmEntity} write-through setter, so they must
 * flush the realm file explicitly. Before that flush existed, such updates were only visible in
 * memory and lost on the next restart. The most prominent victim was the user-profile
 * configuration: Keycloak creates the component and immediately stores the configuration into it
 * via {@code updateComponent} - for the master realm the deliberately relaxed profile written by
 * {@code ApplianceBootstrap}. With the configuration lost, the surviving empty component made
 * Keycloak enforce the strict default profile after a restart, locking the profile-less bootstrap
 * admin out of direct grants with "Account is not fully set up".
 *
 * <p>These tests assert against the written YAML file, which is what survives a restart.
 */
@KeycloakIntegrationTest(config = FileStoreKeycloakServerConfig.class)
public class NestedEntityFlushTest extends KeycloakModelTest {

    private static final String REALM_ID = "flush";

    private static final String USER_PROFILE_CONFIG_KEY = "kc.user.profile.config";

    @InjectRealm(ref = REALM_ID, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @TestOnServer
    public void whenUserProfileConfigurationIsStored_thenItReachesTheRealmFile(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            // setConfiguration creates the component and then stores the configuration into it
            // through updateComponent - the exact sequence Keycloak runs at realm creation
            UserProfileProvider userProfile = session.getProvider(UserProfileProvider.class);
            userProfile.setConfiguration(userProfile.getConfiguration());

            FileRealmEntity written = writtenRealm();
            var component = written.getComponents().stream()
                    .filter(c -> "declarative-user-profile".equals(c.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("user-profile component missing in the realm file"));
            assertThat(component.getConfig()).containsKey(USER_PROFILE_CONFIG_KEY);
            assertThat(component.getConfig().get(USER_PROFILE_CONFIG_KEY).toString())
                    .contains("username");
        });
    }

    @TestOnServer
    public void whenAuthenticatorConfigIsUpdated_thenItReachesTheRealmFile(KeycloakSession testSession) {
        withRealm(testSession, REALM_ID, (session, realm) -> {
            AuthenticatorConfigModel config = new AuthenticatorConfigModel();
            config.setAlias("flush-check");
            config.setConfig(Map.of("value", "before"));
            config = realm.addAuthenticatorConfig(config);

            config.setConfig(Map.of("value", "after"));
            realm.updateAuthenticatorConfig(config);

            FileRealmEntity written = writtenRealm();
            var writtenConfig = written.getAuthenticatorConfigs().stream()
                    .filter(ac -> "flush-check".equals(ac.getAlias()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("authenticator config missing in the realm file"));
            assertThat(writtenConfig.getConfig()).containsEntry("value", "after");
        });
    }

    private static FileRealmEntity writtenRealm() {
        Path realmFile = EntityIO.getPathForIdAndParentPath(REALM_ID, Path.of(TEST_FILESTORE_DIR));
        return parseWrittenFile(realmFile, FileRealmEntity.class);
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
