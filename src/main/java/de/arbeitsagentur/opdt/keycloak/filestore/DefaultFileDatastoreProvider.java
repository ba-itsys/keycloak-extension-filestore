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

import org.keycloak.models.*;
import org.keycloak.storage.MigrationManager;
import org.keycloak.storage.datastore.DefaultDatastoreProvider;

/**
 * Resolves the config-store SPIs through the default-provider election instead of pinning the file
 * providers by id. The built-in jpa providers and caches are disabled (see {@code
 * FileStoreConfigDefaultsSourceFactory}), so the file factories win the election when nothing else
 * contributes. A higher-order extension (e.g. a cassandra area) can take over an SPI, or route it
 * per realm and fall back to the file provider.
 */
public class DefaultFileDatastoreProvider extends DefaultDatastoreProvider {
    private KeycloakSession session;

    public DefaultFileDatastoreProvider(KeycloakSession session) {
        super(null, session);
        this.session = session;
    }

    @Override
    public ClientProvider clients() {
        return session.getProvider(ClientProvider.class);
    }

    @Override
    public ClientProvider clientStorageManager() {
        return clients();
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return session.getProvider(ClientScopeProvider.class);
    }

    @Override
    public ClientScopeProvider clientScopeStorageManager() {
        return clientScopes();
    }

    @Override
    public GroupProvider groups() {
        return session.getProvider(GroupProvider.class);
    }

    @Override
    public GroupProvider groupStorageManager() {
        return groups();
    }

    @Override
    public RealmProvider realms() {
        return session.getProvider(RealmProvider.class);
    }

    @Override
    public RoleProvider roles() {
        return session.getProvider(RoleProvider.class);
    }

    @Override
    public RoleProvider roleStorageManager() {
        return roles();
    }

    @Override
    public IdentityProviderStorageProvider identityProviders() {
        return session.getProvider(IdentityProviderStorageProvider.class);
    }

    @Override
    public MigrationManager getMigrationManager() {
        return new FileStoreMigrationManager();
    }
}
