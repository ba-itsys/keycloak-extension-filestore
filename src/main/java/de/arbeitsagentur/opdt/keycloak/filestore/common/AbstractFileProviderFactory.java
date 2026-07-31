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
package de.arbeitsagentur.opdt.keycloak.filestore.common;

import static org.keycloak.userprofile.DeclarativeUserProfileProviderFactory.PROVIDER_PRIORITY;

import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.component.AmphibianProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;

public abstract class AbstractFileProviderFactory<T extends Provider, V extends AbstractEntity, M>
        implements AmphibianProviderFactory<T>, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "file";

    protected final Logger LOG = Logger.getLogger(getClass());

    public static final AtomicInteger uniqueCounter = new AtomicInteger();
    private final int factoryId = SessionAttributesUtils.grabNewFactoryIdentifier();

    protected final Class<M> modelType;
    private final Class<? extends T> providerType;

    protected AbstractFileProviderFactory(Class<M> modelType, Class<? extends T> providerType) {
        this.modelType = modelType;
        this.providerType = providerType;
    }

    public enum MapProviderObjectType implements InvalidationHandler.InvalidableObjectType {
        CLIENT_BEFORE_REMOVE,
        CLIENT_AFTER_REMOVE,
        CLIENT_SCOPE_BEFORE_REMOVE,
        CLIENT_SCOPE_AFTER_REMOVE,
        GROUP_BEFORE_REMOVE,
        GROUP_AFTER_REMOVE,
        REALM_BEFORE_REMOVE,
        REALM_AFTER_REMOVE,
        RESOURCE_SERVER_BEFORE_REMOVE,
        RESOURCE_SERVER_AFTER_REMOVE,
        ROLE_BEFORE_REMOVE,
        ROLE_AFTER_REMOVE,
        USER_BEFORE_REMOVE,
        USER_AFTER_REMOVE
    }

    /**
     * Creates new instance of a provider.
     *
     * @param session
     * @return See description.
     */
    public abstract T createNew(KeycloakSession session);

    /**
     * Returns instance of a provider. If the instance is already created within the session (it's
     * found in session attributes), it's returned from there, otherwise new instance is created (and
     * stored among the session attributes).
     *
     * @param session
     * @return See description.
     */
    @Override
    public T create(KeycloakSession session) {
        return SessionAttributesUtils.createProviderIfAbsent(session, factoryId, providerType, this::createNew);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Makes the file providers eligible for the default-provider election (Keycloak only elects
     * factories with an order above 0). The built-in jpa providers carry the same order and are
     * disabled by {@code FileStoreConfigDefaultsSourceFactory}, so the file providers win
     * uncontested. Contributed datastore extensions (e.g. the cassandra areas at order 2) can take
     * over an SPI or route it per realm.
     */
    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

    /** Registered only when the file datastore is selected, so the jar alone never shadows anything. */
    @Override
    public boolean isSupported(Scope config) {
        return PROVIDER_ID.equals(Config.getProvider("datastore"));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void init(Scope config) {}
}
