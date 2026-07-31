package de.arbeitsagentur.opdt.keycloak.filestore.identityProvider;

import static org.keycloak.userprofile.DeclarativeUserProfileProviderFactory.PROVIDER_PRIORITY;

import com.google.auto.service.AutoService;
import de.arbeitsagentur.opdt.keycloak.filestore.common.AbstractFileProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.IdentityProviderStorageProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

@AutoService(IdentityProviderStorageProviderFactory.class)
public class FileIdentityProviderStorageProviderFactory
        implements IdentityProviderStorageProviderFactory<FileIdentityProviderStorageProvider>,
                EnvironmentDependentProviderFactory {
    @Override
    public FileIdentityProviderStorageProvider create(KeycloakSession session) {
        return new FileIdentityProviderStorageProvider(session);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return AbstractFileProviderFactory.PROVIDER_ID;
    }

    /**
     * Same election position as the {@link AbstractFileProviderFactory} subclasses. The infinispan
     * idp cache (order 10) is disabled by {@code FileStoreConfigDefaultsSourceFactory}, it cannot
     * work without the realm cache. A contributed idp store (e.g. the cassandra area at order 11)
     * can take over.
     */
    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return AbstractFileProviderFactory.PROVIDER_ID.equals(Config.getProvider("datastore"));
    }
}
