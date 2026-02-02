package de.arbeitsagentur.opdt.keycloak.filestore.compat;

import com.google.auto.service.AutoService;
import java.util.Collections;
import java.util.Set;
import org.keycloak.device.DeviceRepresentationProvider;
import org.keycloak.device.DeviceRepresentationProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.representations.account.DeviceRepresentation;

@AutoService(DeviceRepresentationProviderFactory.class)
public class NullDeviceRepresentationProviderFactory implements DeviceRepresentationProviderFactory {
    public static String PROVIDER_ID = "test";

    @Override
    public DeviceRepresentationProvider create(KeycloakSession session) {
        return new DeviceRepresentationProvider() {
            @Override
            public DeviceRepresentation deviceRepresentation() {
                return new DeviceRepresentation();
            }
        };
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        // Override to remove the default LocalCacheProvider dependency
        return Collections.emptySet();
    }
}
