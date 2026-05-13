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

package de.arbeitsagentur.opdt.keycloak.filestore.group;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.utils.StreamsUtil.paginatedStream;

import de.arbeitsagentur.opdt.keycloak.filestore.SearchPatterns;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractFileProviderFactory;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;

public class FileGroupProvider implements GroupProvider {

    private static final Logger LOG = Logger.getLogger(FileGroupProvider.class);
    private final KeycloakSession session;

    public FileGroupProvider(KeycloakSession session) {
        this.session = session;
    }

    private Function<FileGroupEntity, GroupModel> entityToAdapterFunc(RealmModel realm) {
        return origEntity -> new FileGroupAdapter(session, realm, origEntity) {
            @Override
            public Stream<GroupModel> getSubGroupsStream() {
                return getGroupsByParentId(realm, this.getId());
            }

            @Override
            public OrganizationModel getOrganization() {
                return null;
            }
        };
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        if (realm == null || id == null || id.isBlank()) {
            return null;
        }

        LOG.tracef("getGroupById(%s, %s)%s", realm, id, getShortStackTrace());
        String realmId = realm.getId();
        FileGroupEntity entity = FileGroupStore.read(id, realm.getId());
        return (entity == null || !Objects.equals(realmId, entity.getRealmId()))
                ? null
                : entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public GroupModel getGroupByName(RealmModel realm, GroupModel parent, String name) {
        if (name == null) {
            return null;
        }

        LOG.tracef("getGroupByName(%s, %s)%s", realm, name, getShortStackTrace());
        Stream<FileGroupEntity> groupStream = FileGroupStore.readAll().stream()
                .filter(group -> group.getRealmId().equals(realm.getId()))
                .filter(group -> group.getName().equals(name));

        if (parent != null) {
            groupStream = groupStream.filter(group -> parent.getId().equals(group.getParentId()));
        } else {
            groupStream = groupStream.filter(group -> group.getParentId() == null);
        }
        String groupId = groupStream.findFirst().map(FileGroupEntity::getId).orElse(null);
        return groupId == null ? null : session.groups().getGroupById(realm, groupId);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        return FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
    }

    @Override
    public Stream<GroupModel> getGroupsStream(
            RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        var groups = ids.filter(groupId -> FileGroupStore.exists(groupId, realm.getId()))
                .map(id -> FileGroupStore.read(id, realm.getId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
        if (search != null) {
            groups = groups.filter(entity -> SearchPatterns.insensitiveLike(entity.getName(), "%" + search + "%"));
        }
        return paginatedStream(groups, first, max);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        LOG.tracef("getGroupsCount(%s, %s)%s", realm, onlyTopGroups, getShortStackTrace());
        Stream<GroupModel> groups = FileGroupStore.readAll().stream()
                .filter(group -> group.getRealmId().equals(realm.getId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
        if (Boolean.TRUE.equals(onlyTopGroups)) {
            groups = groups.filter(group -> group.getParentId() == null);
        }
        return groups.count();
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return searchForGroupByNameStream(realm, search, false, null, null).count();
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(
            RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        LOG.tracef("getGroupsByRole(%s, %s, %d, %d)%s", realm, role, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groups = FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getGrantedRoles().contains(role.getId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
        return paginatedStream(groups, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm) {
        LOG.tracef("getTopLevelGroupsStream(%s)%s", realm, getShortStackTrace());
        return getTopLevelGroupsStream(realm, null, null);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        LOG.tracef("getTopLevelGroupsStream(%s, %s, %s)%s", realm, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groups = FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getParentId() == null)
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
        return paginatedStream(groups, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(
            RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        LOG.tracef(
                "getTopLevelGroupsStream(%s, %s, %b, %s, %s)%s",
                realm, search, exact, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groups = FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getParentId() == null) // only parent nodes
                .map(entityToAdapterFunc(realm));

        if (exact != null && exact.equals(Boolean.TRUE)) {
            groups = groups.filter(group -> search.equals(group.getName()));
        } else {
            groups = groups.filter(group -> SearchPatterns.insensitiveLike(group.getName(), "%" + search + "%"));
        }
        groups = groups.sorted(Comparator.comparing(GroupModel::getName));
        return paginatedStream(groups, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> searchForGroupByNameStream(
            RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        LOG.tracef(
                "searchForGroupByNameStream(%s, %s, %s, %b, %d, %d)%s",
                realm, session, search, exact, firstResult, maxResults, getShortStackTrace());
        Stream<GroupModel> groups = FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getParentId() == null) // only parent nodes
                .map(entityToAdapterFunc(realm));

        if (Boolean.TRUE.equals(exact)) {
            groups = groups.filter(group -> search.equals(group.getName()));
        } else {
            groups = groups.filter(group -> SearchPatterns.insensitiveLike(group.getName(), "%" + search + "%"));
        }

        groups = groups.sorted(Comparator.comparing(GroupModel::getName));
        return paginatedStream(groups, firstResult, maxResults)
                .map(GroupModel::getId)
                // todo: this mapping makes no sense at all because we are filtering by group models that
                // have no parent beforehand -> check it again
                // todo: maybe its rather a fallback solution, gotta check this out anyway
                .map(id -> {
                    GroupModel groupById = session.groups().getGroupById(realm, id);
                    while (Objects.nonNull(groupById.getParentId())) {
                        groupById = session.groups().getGroupById(realm, groupById.getParentId());
                    }
                    return groupById;
                })
                .sorted(GroupModel.COMPARE_BY_NAME)
                .distinct();
    }

    @Override
    public Stream<GroupModel> searchGroupsByAttributes(
            RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Stream<FileGroupEntity> groups =
                FileGroupStore.readAll().stream().filter(group -> realm.getId().equals(group.getRealmId()));
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            groups = groups.filter(group -> group.getAttribute(entry.getKey()).contains(entry.getValue()));
        }

        var stream = groups.map(entityToAdapterFunc(realm)).sorted(Comparator.comparing(GroupModel::getName));
        return paginatedStream(stream, firstResult, maxResults);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, String name, GroupModel toParent) {
        return createGroup(realm, id, GroupModel.Type.REALM, name, toParent);
    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, GroupModel.Type type, String name, GroupModel toParent) {
        LOG.tracef("createGroup(%s, %s, %s, %s, %s)%s", realm, id, type, name, toParent, getShortStackTrace());
        FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> name.equals(group.getName()))
                .filter(group -> group.getType().equals(type))
                .filter(group -> toParent == null
                        ? group.getParentId() == null
                        : toParent.getId().equals(group.getParentId()))
                .findAny()
                .ifPresent(group -> {
                    throw new ModelDuplicateException("Group with name '"
                            + name
                            + "' in realm "
                            + realm.getName()
                            + " already exists for requested parent");
                });

        FileGroupEntity entity = new FileGroupEntity();
        if (id == null) {
            entity.setId(name);
        }
        entity.setRealmId(realm.getId());
        entity.setName(name);
        entity.setParentId(toParent == null ? null : toParent.getId());
        if (id != null && FileGroupStore.exists(id, realm.getId())) {
            throw new ModelDuplicateException("Group exists: " + id);
        }

        FileGroupStore.update(entity);
        return entityToAdapterFunc(realm).apply(entity);
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        LOG.tracef("removeGroup(%s, %s)%s", realm, group, getShortStackTrace());
        if (group == null || !FileGroupStore.exists(group.getId(), realm.getId())) {
            return false;
        }

        session.invalidate(AbstractFileProviderFactory.MapProviderObjectType.GROUP_BEFORE_REMOVE, realm, group);
        FileGroupStore.deleteById(group.getId(), realm.getId());
        session.invalidate(AbstractFileProviderFactory.MapProviderObjectType.GROUP_AFTER_REMOVE, realm, group);
        return true;
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        LOG.tracef("moveGroup(%s, %s, %s)%s", realm, group, toParent, getShortStackTrace());
        GroupModel previousParent = group.getParent();
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }

        FileGroupStore.readAll().stream()
                .filter(groupEntity -> realm.getId().equals(groupEntity.getRealmId()))
                .filter(groupEntity -> group.getName().equals(groupEntity.getName()))
                .filter(groupEntity -> toParent == null
                        ? groupEntity.getParentId() == null
                        : toParent.getId().equals(groupEntity.getParentId()))
                .findAny()
                .ifPresent(groupEntity -> {
                    throw new ModelDuplicateException("Group with name '"
                            + group.getName()
                            + "' in realm "
                            + realm.getName()
                            + " already exists for requested parent");
                });
        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }

        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        String newPath = KeycloakModelUtils.buildGroupPath(group);
        String previousPath = KeycloakModelUtils.buildGroupPath(group, previousParent);
        GroupModel.GroupPathChangeEvent event = new GroupModel.GroupPathChangeEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public String getNewPath() {
                return newPath;
            }

            @Override
            public String getPreviousPath() {
                return previousPath;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
        session.getKeycloakSessionFactory().publish(event);
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        LOG.tracef("addTopLevelGroup(%s, %s)%s", realm, subGroup, getShortStackTrace());
        FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getParentId() == null)
                .filter(group -> subGroup.getName().equals(group.getName()))
                .findAny()
                .ifPresent(group -> {
                    throw new ModelDuplicateException(
                            "There is already a top level group named '" + subGroup.getName() + "'");
                });
        subGroup.setParent(null);
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        LOG.tracef("preRemove(%s, %s)%s", realm, role, getShortStackTrace());
        FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> group.getGrantedRoles().contains(role.getId()))
                .map(groupEntity -> session.groups().getGroupById(realm, groupEntity.getId()))
                .forEach(groupModel -> groupModel.deleteRoleMapping(role));
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());
        FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .forEach(groupModel -> FileGroupStore.deleteById(groupModel.getId(), realm.getId()));
    }

    @Override
    public void close() {
        // nothing to close
    }

    private Stream<GroupModel> getGroupsByParentId(RealmModel realm, String parentId) {
        LOG.tracef("getGroupsByParentId(%s)%s", parentId, getShortStackTrace());
        return FileGroupStore.readAll().stream()
                .filter(group -> realm.getId().equals(group.getRealmId()))
                .filter(group -> parentId.equals(group.getParentId()))
                .map(entityToAdapterFunc(realm))
                .sorted(Comparator.comparing(GroupModel::getName));
    }
}
