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

public class DefaultFileDatastoreProvider extends DefaultDatastoreProvider {
    private KeycloakSession session;

    public DefaultFileDatastoreProvider(KeycloakSession session) {
        super(null, session);
        this.session = session;
    }

    @Override
    public ClientProvider clients() {
        return session.getProvider(ClientProvider.class, "file");
    }

    @Override
    public ClientProvider clientStorageManager() {
        return clients();
    }

    @Override
    public ClientScopeProvider clientScopes() {
        return session.getProvider(ClientScopeProvider.class, "file");
    }

    @Override
    public ClientScopeProvider clientScopeStorageManager() {
        return clientScopes();
    }

    @Override
    public GroupProvider groups() {
        return session.getProvider(GroupProvider.class, "file");
    }

    @Override
    public GroupProvider groupStorageManager() {
        return groups();
    }

    @Override
    public RealmProvider realms() {
        return session.getProvider(RealmProvider.class, "file");
    }

    @Override
    public RoleProvider roles() {
        return session.getProvider(RoleProvider.class, "file");
    }

    @Override
    public RoleProvider roleStorageManager() {
        return roles();
    }

    @Override
    public IdentityProviderStorageProvider identityProviders() {
        return session.getProvider(IdentityProviderStorageProvider.class, "file");
    }

    @Override
    public MigrationManager getMigrationManager() {
        return new FileStoreMigrationManager();
    }
}
