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

import static de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractFileProviderFactory.MapProviderObjectType.*;

import com.google.auto.service.AutoService;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractFileProviderFactory;
import java.util.stream.Collectors;
import org.keycloak.models.*;
import org.keycloak.provider.InvalidationHandler;

/**
 * @author mhajas
 */
@AutoService(GroupProviderFactory.class)
public class FileGroupProviderFactory
        extends AbstractFileProviderFactory<FileGroupProvider, FileGroupEntity, GroupModel>
        implements GroupProviderFactory<FileGroupProvider>, InvalidationHandler {

    public FileGroupProviderFactory() {
        super(GroupModel.class, FileGroupProvider.class);
    }

    @Override
    public FileGroupProvider createNew(KeycloakSession session) {
        return new FileGroupProvider(session);
    }

    @Override
    public String getHelpText() {
        return "Group provider";
    }

    @Override
    public void invalidate(KeycloakSession session, InvalidableObjectType type, Object... params) {
        if (type == REALM_BEFORE_REMOVE) {
            create(session).preRemove((RealmModel) params[0]);
        } else if (type == ROLE_BEFORE_REMOVE) {
            create(session).preRemove((RealmModel) params[0], (RoleModel) params[1]);
        } else if (type == GROUP_BEFORE_REMOVE) {
            RealmModel realm = (RealmModel) params[0];
            GroupModel group = (GroupModel) params[1];
            realm.removeDefaultGroup(group);
            group.getSubGroupsStream()
                    .collect(Collectors.toSet())
                    .forEach(subGroup -> create(session).removeGroup(realm, subGroup));
        } else if (type == GROUP_AFTER_REMOVE) {
            session.getKeycloakSessionFactory().publish(new GroupModel.GroupRemovedEvent() {
                @Override
                public RealmModel getRealm() {
                    return (RealmModel) params[0];
                }

                @Override
                public GroupModel getGroup() {
                    return (GroupModel) params[1];
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }
    }
}
