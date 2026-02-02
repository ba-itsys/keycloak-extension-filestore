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

import de.arbeitsagentur.opdt.keycloak.filestore.client.FileClientEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.clientscope.FileClientScopeEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.group.FileGroupEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.realm.FileRealmEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.role.FileRoleEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

public final class EntityStore {
    private static final Logger LOG = Logger.getLogger(EntityStore.class);

    private static final Map<Path, AbstractEntity> STORE = new ConcurrentHashMap<>();
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static volatile boolean initialized = false;

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (EntityStore.class) {
            if (initialized) {
                return;
            }
            doInitialize();
            initialized = true;
        }
    }

    private static void doInitialize() {
        Path dataDirectory = EntityIO.getRootDirectory();
        try (Stream<Path> realmsStream = Files.walk(dataDirectory, 1)) {
            realmsStream
                    .filter(EntityIO::canParseFile)
                    .forEach(path -> STORE.put(path, EntityIO.parseFile(path, FileRealmEntity.class)));

            for (AbstractEntity abstractEntity : STORE.values()) {
                if (abstractEntity instanceof FileRealmEntity realm) {
                    Path clients = dataDirectory.resolve(realm.getId()).resolve("clients");
                    if (clients.toFile().exists()) {
                        try (Stream<Path> clientsStream = Files.walk(clients, 1)) {
                            clientsStream.filter(EntityIO::canParseFile).forEach(path -> {
                                FileClientEntity entity = EntityIO.parseFile(path, FileClientEntity.class);
                                entity.setRealmId(realm.getId());
                                STORE.put(path, entity);
                            });
                        }
                    }

                    Path scopes = dataDirectory.resolve(realm.getId()).resolve("client-scopes");
                    if (scopes.toFile().exists()) {
                        try (Stream<Path> clientScopesStream = Files.walk(scopes, 1)) {
                            clientScopesStream.filter(EntityIO::canParseFile).forEach(path -> {
                                FileClientScopeEntity entity = EntityIO.parseFile(path, FileClientScopeEntity.class);
                                entity.setRealmId(realm.getId());
                                STORE.put(path, entity);
                            });
                        }
                    }
                    Path groups = dataDirectory.resolve(realm.getId()).resolve("groups");
                    if (groups.toFile().exists()) {
                        try (Stream<Path> groupsStream = Files.walk(groups, 1)) {
                            groupsStream.filter(EntityIO::canParseFile).forEach(path -> {
                                FileGroupEntity entity = EntityIO.parseFile(path, FileGroupEntity.class);
                                entity.setRealmId(realm.getId());
                                STORE.put(path, entity);
                            });
                        }
                    }

                    Path roles = dataDirectory.resolve(realm.getId()).resolve("roles");
                    if (roles.toFile().exists()) {
                        try (Stream<Path> rolesStream =
                                Files.walk(roles, 10)) { // can be deeply nested because of composite roles
                            rolesStream.filter(EntityIO::canParseFile).forEach(path -> {
                                FileRoleEntity entity = EntityIO.parseFile(path, FileRoleEntity.class);
                                entity.setRealmId(realm.getId());
                                STORE.put(path, entity);
                            });
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void delete(Path path) {
        ensureInitialized();
        LOCK.writeLock().lock();

        try {
            STORE.remove(path);
            Files.delete(path);
            EntityIO.deleteParentDirectoryIfEmpty(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static <E extends AbstractEntity & UpdatableEntity> void write(Path path, E entity) {
        ensureInitialized();
        LOCK.writeLock().lock();
        try {
            EntityIO.writeToFile(entity, path);
            STORE.put(path, entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static <E extends AbstractEntity & UpdatableEntity> List<E> getAll(Class<E> interfaceOfEntity) {
        ensureInitialized();
        LOCK.readLock().lock();

        try {
            return STORE.values().stream()
                    .filter(v -> interfaceOfEntity.isAssignableFrom(v.getClass()))
                    .map(interfaceOfEntity::cast)
                    .collect(Collectors.toCollection(ArrayList::new));
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static <E extends AbstractEntity & UpdatableEntity> E get(Path fileName) {
        ensureInitialized();
        LOCK.readLock().lock();

        try {
            return (E) STORE.get(fileName);
        } finally {
            LOCK.readLock().unlock();
        }
    }

    // For testing purposes - allows resetting the store state
    static void reset() {
        synchronized (EntityStore.class) {
            STORE.clear();
            initialized = false;
        }
    }
}
