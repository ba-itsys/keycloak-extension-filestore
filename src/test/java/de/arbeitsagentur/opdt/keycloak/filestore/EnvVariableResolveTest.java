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

package de.arbeitsagentur.opdt.keycloak.filestore;

import static org.assertj.core.api.Assertions.assertThat;

import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

class EnvVariableResolveTest {

    @Test
    void givenEnvVariable_thenVariableResolutionIsCorrect() throws Exception {
        Path realmFile = Files.createTempFile("realm", ".yaml");
        Files.writeString(realmFile, """
                id: master
                name: master
                accountTheme: ${SHOULD_BE_RESOLVED}
                adminTheme: ${SHOULD_NOT_BE_RESOLVED}
                emailTheme: ${UNSET_VARIABLE:-defaultValue}
                """);

        EnvironmentVariables env = new EnvironmentVariables("SHOULD_BE_RESOLVED", "i am resolved!");
        env.execute(() -> {
            FileRealmEntity realm = EntityIO.yamlParseFile(realmFile, FileRealmEntity.class);
            assertThat(realm.getAccountTheme()).isEqualTo("i am resolved!");
            assertThat(realm.getAdminTheme()).isEqualTo("${SHOULD_NOT_BE_RESOLVED}");
            assertThat(realm.getEmailTheme()).isEqualTo("defaultValue");
        });
    }
}
