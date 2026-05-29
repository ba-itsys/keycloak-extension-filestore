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

package de.arbeitsagentur.opdt.keycloak.filestore.client;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

import de.arbeitsagentur.opdt.keycloak.filestore.SearchPatterns;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractFileProviderFactory;
import de.arbeitsagentur.opdt.keycloak.filestore.common.TimeAdapter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.ClientModel.ClientUpdatedEvent;

public class FileClientProvider implements ClientProvider {

    private static final Logger LOG = Logger.getLogger(FileClientProvider.class);
    private final KeycloakSession session;
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> clientRegisteredNodesStore;

    public FileClientProvider(
            KeycloakSession session, ConcurrentMap<String, ConcurrentMap<String, Long>> clientRegisteredNodesStore) {
        this.session = session;
        this.clientRegisteredNodesStore = clientRegisteredNodesStore;
    }

    private ClientUpdatedEvent clientUpdatedEvent(ClientModel c) {
        return new ClientUpdatedEvent() {
            @Override
            public ClientModel getUpdatedClient() {
                return c;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    private <T extends FileClientEntity> Function<T, ClientModel> entityToAdapterFunc(RealmModel realm) {
        // Clone entity before returning back, to avoid giving away a reference to the live object to
        // the caller
        return origEntity -> new FileClientAdapter(session, realm, origEntity) {
            @Override
            public void updateClient() {
                LOG.tracef("updateClient(%s)%s", realm, origEntity.getId(), getShortStackTrace());
                session.getKeycloakSessionFactory().publish(clientUpdatedEvent(this));
            }

            /** This is runtime information and should have never been part of the adapter */
            @Override
            public Map<String, Integer> getRegisteredNodes() {
                return Collections.unmodifiableMap(getMapForEntity().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(e.getValue()))));
            }

            @Override
            public void registerNode(String nodeHost, int registrationTime) {
                getMapForEntity()
                        .put(
                                nodeHost,
                                TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(registrationTime));
            }

            @Override
            public void unregisterNode(String nodeHost) {
                getMapForEntity().remove(nodeHost);
            }

            private ConcurrentMap<String, Long> getMapForEntity() {
                return clientRegisteredNodesStore.computeIfAbsent(entity.getId(), k -> new ConcurrentHashMap<>());
            }
        };
    }

    private boolean isEntityPartOfRealm(RealmModel realm, FileClientEntity entity) {
        return Objects.equals(realm.getId(), entity.getRealmId());
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        Stream<ClientModel> clients = getClientsStream(realm);
        return paginatedStream(clients, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(ClientModel::getClientId));
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        LOG.tracef("addClient(%s, %s, %s)%s", realm, id, clientId, getShortStackTrace());

        if (clientId == null) {
            throw new IllegalArgumentException("clientId cannot be null");
        }

        if (id != null && FileClientStore.exists(id, realm.getId())) {
            throw new ModelDuplicateException("Client with same id exists: " + id);
        }

        if (getClientByClientId(realm, clientId) != null) {
            throw new ModelDuplicateException(
                    "Client with same clientId in realm " + realm.getName() + " exists: " + clientId);
        }
        FileClientEntity entity = new FileClientEntity();
        entity.setId(clientId);
        entity.setRealmId(realm.getId());
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);

        final ClientModel resource = entityToAdapterFunc(realm).apply(entity);
        session.getKeycloakSessionFactory().publish((ClientModel.ClientCreationEvent) () -> resource);
        resource.updateClient();

        FileClientStore.update(entity);
        return resource;
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        return FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .filter(client -> Boolean.TRUE.equals(client.isAlwaysDisplayInConsole()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(ClientModel::getClientId));
    }

    @Override
    public void removeClients(RealmModel realm) {
        LOG.tracef("removeClients(%s)%s", realm, getShortStackTrace());
        getClientsStream(realm)
                .map(ClientModel::getId)
                .collect(Collectors.toSet()) // This is necessary to read out all the client IDs before removing the
                // clients
                .forEach(cid -> removeClient(realm, cid));
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {
        if (id == null) return false;

        LOG.tracef("removeClient(%s, %s)%s", realm, id, getShortStackTrace());

        final ClientModel client = getClientById(realm, id);
        if (client == null) return false;

        session.invalidate(AbstractFileProviderFactory.MapProviderObjectType.CLIENT_BEFORE_REMOVE, realm, client);
        FileClientStore.deleteById(id, realm.getId());
        session.invalidate(AbstractFileProviderFactory.MapProviderObjectType.CLIENT_AFTER_REMOVE, client);
        return true;
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        return FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .count();
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        LOG.tracef("getClientById(%s, %s)%s", realm, id, getShortStackTrace());

        FileClientEntity entity = FileClientStore.read(id, realm.getId());
        if (entity != null && isEntityPartOfRealm(realm, entity)) {
            return entityToAdapterFunc(realm).apply(entity);
        } else {
            return null;
        }
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        if (clientId == null) {
            return null;
        }

        return FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .filter(client -> clientId.equals(client.getClientId()))
                .map(entityToAdapterFunc(realm))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(
            RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        if (clientId == null) {
            return Stream.empty();
        }

        Stream<ClientModel> clients = FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .filter(client -> SearchPatterns.insensitiveLike(client.getClientId(), "%" + clientId + "%"))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(ClientModel::getClientId));

        return paginatedStream(clients, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(
            RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Stream<ClientModel> clients = FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(ClientModel::getClientId));

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            clients = clients.filter(client -> {
                var attr = client.getAttribute(entry.getKey());
                return attr != null && attr.contains(entry.getValue());
            });
        }

        return paginatedStream(clients, firstResult, maxResults);
    }

    @Override
    public void addClientScopes(
            RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        final String id = client.getId();
        FileClientEntity entity = FileClientStore.read(id, realm.getId());
        if (entity == null) return;

        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? "openid-connect" : client.getProtocol();
        LOG.tracef(
                "addClientScopes(%s, %s, %s, %b)%s", realm, client, clientScopes, defaultScope, getShortStackTrace());
        Map<String, ClientScopeModel> existingClientScopes = getClientScopes(realm, client, true);
        existingClientScopes.putAll(getClientScopes(realm, client, false));
        clientScopes.stream()
                .filter(clientScope -> !existingClientScopes.containsKey(clientScope.getName()))
                .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
                .forEach(clientScope -> entity.setClientScope(clientScope.getId(), defaultScope));
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        if (client == null || clientScope == null) return;

        final String id = client.getId();
        FileClientEntity entity = FileClientStore.read(id, realm.getId());
        if (entity == null) return;

        LOG.tracef("removeClientScope(%s, %s, %s)%s", realm, client, clientScope, getShortStackTrace());
        entity.removeClientScope(clientScope.getId());
    }

    @Override
    public void addClientScopeToAllClients(RealmModel realm, ClientScopeModel clientScope, boolean defaultClientScope) {
        FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .forEach(client -> client.setClientScope(clientScope.getId(), defaultClientScope));
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScopes) {
        final String id = client.getId();
        FileClientEntity entity = FileClientStore.read(id, realm.getId());

        if (entity == null) return null;
        // Defaults to openid-connect
        String clientProtocol = client.getProtocol() == null ? "openid-connect" : client.getProtocol();
        LOG.tracef("getClientScopes(%s, %s, %b)%s", realm, client, defaultScopes, getShortStackTrace());
        return entity.getClientScopes(defaultScopes)
                .map(clientScopeId -> session.clientScopes().getClientScopeById(realm, clientScopeId))
                .filter(Objects::nonNull)
                .filter(clientScope -> Objects.equals(clientScope.getProtocol(), clientProtocol))
                .collect(Collectors.toMap(ClientScopeModel::getName, Function.identity()));
    }

    /**
     * @deprecated Do not use, this is only to support a deprecated logout endpoint and will vanish
     *     with its removal
     */
    @Deprecated(forRemoval = true)
    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        try (Stream<FileClientEntity> st = FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .filter(client -> Boolean.TRUE.equals(client.isEnabled()))
                .sorted(Comparator.comparing(FileClientEntity::getClientId))) {
            return st.filter(mce -> mce.getRedirectUris() != null
                            && !mce.getRedirectUris().isEmpty())
                    .collect(Collectors.toMap(
                            mce -> entityToAdapterFunc(realm).apply(mce), mce -> new HashSet<>(mce.getRedirectUris())));
        }
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        try (Stream<FileClientEntity> toRemove = FileClientStore.readAll().stream()
                .filter(client -> realm.getId().equals(client.getRealmId()))
                .filter(client -> client.getScopeMappings().contains(role.getId()))
                .sorted(Comparator.comparing(FileClientEntity::getClientId))) {
            toRemove.forEach(clientEntity -> clientEntity.removeScopeMapping(role.getId()));
        }
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());
        FileClientStore.deleteByRealmId(realm.getId());
    }

    @Override
    public void close() {}
}
