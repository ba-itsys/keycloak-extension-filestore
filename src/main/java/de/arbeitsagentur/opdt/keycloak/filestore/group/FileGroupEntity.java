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

import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractEntity;
import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.keycloak.models.GroupModel;

public class FileGroupEntity implements AbstractEntity, UpdatableEntity {

    private String id;
    private Map<String, Object> attributes = new HashMap<>();
    private boolean isUpdated = false;
    private String name;
    private String parentId;
    private String realmId;
    private GroupModel.Type type;
    private String description;
    private List<String> grantedRoles = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        FileGroupStore.update(this);
    }

    public void setAttribute(String key, List<String> singleListValue) {
        this.attributes.put(key, singleListValue.get(0));
        FileGroupStore.update(this);
    }

    public void removeAttribute(String key) {
        this.attributes.remove(key);
        FileGroupStore.update(this);
    }

    public Map<String, List<String>> getMultiValuedAttributes() {
        return attributes.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().toString()))
                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), List.of(e.getValue())), HashMap::putAll);
    }

    public List<String> getAttribute(String name) {
        Object attr = this.attributes.get(name);
        return attr == null ? List.of() : List.of(attr.toString());
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public void setUpdated(boolean updated) {
        isUpdated = updated;
        FileGroupStore.update(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        FileGroupStore.update(this);
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
        FileGroupStore.update(this);
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public List<String> getGrantedRoles() {
        return grantedRoles;
    }

    public void setGrantedRoles(List<String> grantedRoles) {
        this.grantedRoles = grantedRoles;
        FileGroupStore.update(this);
    }

    public void addGrantedRole(String role) {
        this.grantedRoles.add(role);
        FileGroupStore.update(this);
    }

    public void removeGrantedRole(String role) {
        this.grantedRoles.remove(role);
        FileGroupStore.update(this);
    }

    public GroupModel.Type getType() {
        if (type == null) {
            return GroupModel.Type.REALM;
        }

        return type;
    }

    public void setType(GroupModel.Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
