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

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.DatastoreProviderFactory;

@AutoService(DatastoreProviderFactory.class)
public class DefaultFileDatastoreProviderFactory implements DatastoreProviderFactory {
    @Override
    public DatastoreProvider create(KeycloakSession session) {
        return new DefaultFileDatastoreProvider(session);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (!Profile.isFeatureEnabled(Profile.Feature.STATELESS)) {
            throw new IllegalStateException(
                    "The file datastore requires the 'stateless' feature. Start Keycloak with --features=stateless.");
        }
    }

    @Override
    public void close() {}

    @Override
    public String getId() {
        return "file";
    }
}
