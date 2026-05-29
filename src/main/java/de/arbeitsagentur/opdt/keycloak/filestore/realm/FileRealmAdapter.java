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

package de.arbeitsagentur.opdt.keycloak.filestore.realm;

import static java.util.Objects.nonNull;

import de.arbeitsagentur.opdt.keycloak.filestore.common.TimeAdapter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.*;
import org.keycloak.models.utils.ComponentUtil;
import org.keycloak.representations.idm.RealmRepresentation;

public class FileRealmAdapter extends AbstractRealmModel<FileRealmEntity> implements StorageProviderRealmModel {

    private static final Logger LOG = Logger.getLogger(FileRealmAdapter.class);
    private static final String ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN = "actionTokenGeneratedByUserLifespan";
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "defaultSignatureAlgorithm";
    private static final String BRUTE_FORCE_PROTECTED = "bruteForceProtected";
    private static final String PERMANENT_LOCKOUT = "permanentLockout";
    private static final String MAX_FAILURE_WAIT_SECONDS = "maxFailureWaitSeconds";
    private static final String WAIT_INCREMENT_SECONDS = "waitIncrementSeconds";
    private static final String QUICK_LOGIN_CHECK_MILLISECONDS = "quickLoginCheckMilliSeconds";
    private static final String MINIMUM_QUICK_LOGIN_WAIT_SECONDS = "minimumQuickLoginWaitSeconds";
    private static final String MAX_DELTA_SECONDS = "maxDeltaTimeSeconds";
    private static final String FAILURE_FACTOR = "failureFactor";

    private static final String SCIM_API_ENABLED = "scimApiEnabled";
    private static final String MAX_SECONDARY_AUTH_FAILURES = "maxSecondaryAuthFailures";
    private static final String MAX_TEMPORARY_LOCKOUTS = "maxTemporaryLockouts";
    private static final String BRUTE_FORCE_STRATEGY = "bruteForceStrategy";
    public static final String ADMIN_PERMISSIONS_CLIENT_ID = "adminPermissionsClientId";

    private PasswordPolicy passwordPolicy;

    public FileRealmAdapter(KeycloakSession session, FileRealmEntity entity) {
        super(session, entity);
    }

    @Override
    public String getId() {
        return entity.getName();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String displayName) {
        entity.setDisplayName(displayName);
    }

    @Override
    public String getDisplayNameHtml() {
        return entity.getDisplayNameHtml();
    }

    @Override
    public void setDisplayNameHtml(String displayNameHtml) {
        entity.setDisplayNameHtml(displayNameHtml);
    }

    @Override
    public boolean isEnabled() {
        Boolean enabled = entity.isEnabled();
        return enabled == null ? false : enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public SslRequired getSslRequired() {
        String sslRequired = entity.getSslRequired();
        return sslRequired == null ? null : SslRequired.valueOf(sslRequired);
    }

    @Override
    public void setSslRequired(SslRequired sslRequired) {
        entity.setSslRequired(sslRequired.name());
    }

    @Override
    public boolean isRegistrationAllowed() {
        Boolean is = entity.isRegistrationAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setRegistrationAllowed(boolean registrationAllowed) {
        entity.setRegistrationAllowed(registrationAllowed);
    }

    @Override
    public boolean isRegistrationEmailAsUsername() {
        Boolean is = entity.isRegistrationEmailAsUsername();
        return is == null ? false : is;
    }

    @Override
    public void setRegistrationEmailAsUsername(boolean registrationEmailAsUsername) {
        entity.setRegistrationEmailAsUsername(registrationEmailAsUsername);
    }

    @Override
    public boolean isRememberMe() {
        Boolean is = entity.isRememberMe();
        return is == null ? false : is;
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        entity.setRememberMe(rememberMe);
    }

    @Override
    public boolean isEditUsernameAllowed() {
        Boolean is = entity.isEditUsernameAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setEditUsernameAllowed(boolean editUsernameAllowed) {
        entity.setEditUsernameAllowed(editUsernameAllowed);
    }

    @Override
    public boolean isUserManagedAccessAllowed() {
        Boolean is = entity.isAllowUserManagedAccess();
        return is == null ? false : is;
    }

    @Override
    public void setUserManagedAccessAllowed(boolean userManagedAccessAllowed) {
        entity.setAllowUserManagedAccess(userManagedAccessAllowed);
    }

    @Override
    public boolean isOrganizationsEnabled() {
        Boolean is = entity.isOrganizationsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setOrganizationsEnabled(boolean organizationsEnabled) {
        entity.setOrganizationsEnabled(organizationsEnabled);
    }

    @Override
    public boolean isAdminPermissionsEnabled() {
        Boolean value = entity.isAdminPermissionsEnabled();
        return value == null ? false : value;
    }

    @Override
    public void setAdminPermissionsEnabled(boolean adminPermissionsEnabled) {
        entity.setAdminPermissionsEnabled(adminPermissionsEnabled);
    }

    @Override
    public boolean isVerifiableCredentialsEnabled() {
        Boolean value = entity.isVerifiableCredentialsEnabled();
        return value == null ? false : value;
    }

    @Override
    public void setVerifiableCredentialsEnabled(boolean verifiableCredentialsEnabled) {
        entity.setVerifiableCredentialsEnabled(verifiableCredentialsEnabled);
    }

    @Override
    public void setAttribute(String name, String value) {
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, List<String>> attrs = entity.retrieveListAttributes();
        return attrs == null || attrs.isEmpty()
                ? Collections.emptyMap()
                : attrs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    if (entry.getValue().isEmpty()) {
                        return null;
                    } else if (entry.getValue().size() > 1) {
                        /**
                         * This should not be possible anymore since no multi-valued reading is
                         * supported. The current interface of {@link
                         * FileRealmEntity#retrieveListAttributes()} still indicates a multi valued
                         * map due to legacy but in reality it returns a map from key to singleton
                         * list. This should be changed in upcoming steps once the data store has
                         * proven to be stable.
                         */
                        LOG.warnf(
                                "Realm '%s' has attribute '%s' with %d values, retrieving only the first",
                                getId(), entry.getKey(), entry.getValue().size());
                    }
                    return entry.getValue().get(0);
                }));
    }

    @Override
    public boolean isVerifyEmail() {
        Boolean is = entity.isVerifyEmail();
        return is == null ? false : is;
    }

    @Override
    public void setVerifyEmail(boolean verifyEmail) {
        entity.setVerifyEmail(verifyEmail);
    }

    @Override
    public boolean isLoginWithEmailAllowed() {
        Boolean is = entity.isLoginWithEmailAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setLoginWithEmailAllowed(boolean loginWithEmailAllowed) {
        entity.setLoginWithEmailAllowed(loginWithEmailAllowed);
    }

    @Override
    public boolean isDuplicateEmailsAllowed() {
        Boolean is = entity.isDuplicateEmailsAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setDuplicateEmailsAllowed(boolean duplicateEmailsAllowed) {
        entity.setDuplicateEmailsAllowed(duplicateEmailsAllowed);
    }

    @Override
    public boolean isResetPasswordAllowed() {
        Boolean is = entity.isResetPasswordAllowed();
        return is == null ? false : is;
    }

    @Override
    public void setResetPasswordAllowed(boolean resetPasswordAllowed) {
        entity.setResetPasswordAllowed(resetPasswordAllowed);
    }

    @Override
    public boolean isRevokeRefreshToken() {
        Boolean is = entity.isRevokeRefreshToken();
        return is == null ? false : is;
    }

    @Override
    public void setRevokeRefreshToken(boolean revokeRefreshToken) {
        entity.setRevokeRefreshToken(revokeRefreshToken);
    }

    @Override
    public int getRefreshTokenMaxReuse() {
        Integer i = entity.getRefreshTokenMaxReuse();
        return i == null ? 0 : i;
    }

    @Override
    public void setRefreshTokenMaxReuse(int revokeRefreshTokenCount) {
        entity.setRefreshTokenMaxReuse(revokeRefreshTokenCount);
    }

    @Override
    public int getSsoSessionIdleTimeout() {
        Integer i = entity.getSsoSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionIdleTimeout(int seconds) {
        entity.setSsoSessionIdleTimeout(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespan() {
        Integer i = entity.getSsoSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionMaxLifespan(int seconds) {
        entity.setSsoSessionMaxLifespan(seconds);
    }

    @Override
    public int getSsoSessionIdleTimeoutRememberMe() {
        Integer i = entity.getSsoSessionIdleTimeoutRememberMe();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionIdleTimeoutRememberMe(int seconds) {
        entity.setSsoSessionIdleTimeoutRememberMe(seconds);
    }

    @Override
    public int getSsoSessionMaxLifespanRememberMe() {
        Integer i = entity.getSsoSessionMaxLifespanRememberMe();
        return i == null ? 0 : i;
    }

    @Override
    public void setSsoSessionMaxLifespanRememberMe(int seconds) {
        entity.setSsoSessionMaxLifespanRememberMe(seconds);
    }

    @Override
    public int getOfflineSessionIdleTimeout() {
        Integer i = entity.getOfflineSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setOfflineSessionIdleTimeout(int seconds) {
        entity.setOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getAccessTokenLifespan() {
        Integer i = entity.getAccessTokenLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public int getClientSessionIdleTimeout() {
        Integer i = entity.getClientSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientSessionIdleTimeout(int seconds) {
        entity.setClientSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientSessionMaxLifespan() {
        Integer i = entity.getClientSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientSessionMaxLifespan(int seconds) {
        entity.setClientSessionMaxLifespan(seconds);
    }

    @Override
    public int getClientOfflineSessionIdleTimeout() {
        Integer i = entity.getClientOfflineSessionIdleTimeout();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientOfflineSessionIdleTimeout(int seconds) {
        entity.setClientOfflineSessionIdleTimeout(seconds);
    }

    @Override
    public int getClientOfflineSessionMaxLifespan() {
        Integer i = entity.getClientOfflineSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setClientOfflineSessionMaxLifespan(int seconds) {
        entity.setClientOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public void setAccessTokenLifespan(int seconds) {
        entity.setAccessTokenLifespan(seconds);
    }

    @Override
    public int getAccessTokenLifespanForImplicitFlow() {
        Integer i = entity.getAccessTokenLifespanForImplicitFlow();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessTokenLifespanForImplicitFlow(int seconds) {
        entity.setAccessTokenLifespanForImplicitFlow(seconds);
    }

    @Override
    public int getAccessCodeLifespan() {
        Integer i = entity.getAccessCodeLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespan(int seconds) {
        entity.setAccessCodeLifespan(seconds);
    }

    @Override
    public int getAccessCodeLifespanUserAction() {
        Integer i = entity.getAccessCodeLifespanUserAction();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespanUserAction(int seconds) {
        entity.setAccessCodeLifespanUserAction(seconds);
    }

    @Override
    public int getAccessCodeLifespanLogin() {
        Integer i = entity.getAccessCodeLifespanLogin();
        return i == null ? 0 : i;
    }

    @Override
    public void setAccessCodeLifespanLogin(int seconds) {
        entity.setAccessCodeLifespanLogin(seconds);
    }

    @Override
    public int getActionTokenGeneratedByAdminLifespan() {
        Integer i = entity.getActionTokenGeneratedByAdminLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setActionTokenGeneratedByAdminLifespan(int seconds) {
        entity.setActionTokenGeneratedByAdminLifespan(seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan() {
        return getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(int seconds) {
        setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN, seconds);
    }

    @Override
    public int getActionTokenGeneratedByUserLifespan(String actionTokenType) {
        if (actionTokenType == null
                || getAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType) == null) {
            return getActionTokenGeneratedByUserLifespan();
        }
        return getAttribute(
                ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, getAccessCodeLifespanUserAction());
    }

    @Override
    public void setActionTokenGeneratedByUserLifespan(String actionTokenType, Integer seconds) {
        if (actionTokenType != null && !actionTokenType.isEmpty() && seconds != null) {
            setAttribute(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "." + actionTokenType, seconds);
        }
    }

    @Override
    public Map<String, Integer> getUserActionTokenLifespans() {
        Map<String, List<String>> attrs = entity.retrieveListAttributes();
        if (attrs == null || attrs.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> tokenLifespans = attrs.entrySet().stream()
                .filter(Objects::nonNull)
                .filter(entry -> nonNull(entry.getValue()) && !entry.getValue().isEmpty())
                .filter(entry -> entry.getKey().startsWith(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN + "."))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(ACTION_TOKEN_GENERATED_BY_USER_LIFESPAN.length() + 1),
                        entry -> Integer.valueOf(entry.getValue().get(0))));
        return Collections.unmodifiableMap(tokenLifespans);
    }

    @Override
    public Stream<RequiredCredentialModel> getRequiredCredentialsStream() {
        Set<FileRequiredCredentialEntity> rCEs = new HashSet<>(entity.getRequiredCredentials());
        return rCEs == null ? Stream.empty() : rCEs.stream().map(FileRequiredCredentialEntity::toModel);
    }

    @Override
    public void addRequiredCredential(String cred) {
        RequiredCredentialModel model = RequiredCredentialModel.BUILT_IN.get(cred);
        if (model == null) {
            throw new IllegalArgumentException("Unknown credential type " + cred);
        }
        if (getRequiredCredentialsStream()
                .anyMatch(credential -> Objects.equals(model.getType(), credential.getType()))) {
            throw new ModelDuplicateException("A Required Credential with given type already exists.");
        }
        entity.addRequiredCredential(FileRequiredCredentialEntity.fromModel(model));
    }

    @Override
    public void updateRequiredCredentials(Set<String> credentials) {
        Set<FileRequiredCredentialEntity> requiredCredentialEntities = new HashSet<>(entity.getRequiredCredentials());
        Consumer<FileRequiredCredentialEntity> updateCredentialFnc = e -> {
            Optional<FileRequiredCredentialEntity> existingEntity = requiredCredentialEntities.stream()
                    .filter(existing -> Objects.equals(e.getType(), existing.getType()))
                    .findFirst();
            if (existingEntity.isPresent()) {
                updateRequiredCredential(existingEntity.get(), e);
            } else {
                entity.addRequiredCredential(e);
            }
        };
        credentials.stream()
                .map(RequiredCredentialModel.BUILT_IN::get)
                .peek(c -> {
                    if (c == null) {
                        throw new IllegalArgumentException("Unknown credential type " + c.getType());
                    }
                })
                .map(FileRequiredCredentialEntity::fromModel)
                .forEach(updateCredentialFnc);
    }

    private void updateRequiredCredential(
            FileRequiredCredentialEntity existing, FileRequiredCredentialEntity newValue) {
        existing.setFormLabel(newValue.getFormLabel());
        existing.setInput(newValue.isInput());
        existing.setSecret(newValue.isSecret());
    }

    @Override
    public PasswordPolicy getPasswordPolicy() {
        if (passwordPolicy == null) {
            passwordPolicy = PasswordPolicy.parse(session, entity.getPasswordPolicy());
        }
        return passwordPolicy;
    }

    @Override
    public void setPasswordPolicy(PasswordPolicy policy) {
        this.passwordPolicy = policy;
        entity.setPasswordPolicy(policy.toString());
    }

    @Override
    public OTPPolicy getOTPPolicy() {
        FileOTPPolicyEntity policy = entity.getOtpPolicy();
        return policy == null ? OTPPolicy.DEFAULT_POLICY : FileOTPPolicyEntity.toModel(policy);
    }

    @Override
    public void setOTPPolicy(OTPPolicy policy) {
        entity.setOtpPolicy(FileOTPPolicyEntity.fromModel(policy));
    }

    @Override
    public RoleModel getRoleById(String id) {
        return session.roles().getRoleById(this, id);
    }

    @Override
    public Stream<GroupModel> getDefaultGroupsStream() {
        Set<String> gIds = new HashSet<>(entity.getDefaultGroupIds());
        return gIds == null ? Stream.empty() : gIds.stream().map(this::getGroupById);
    }

    @Override
    public void addDefaultGroup(GroupModel group) {
        entity.addDefaultGroupId(group.getId());
    }

    @Override
    public void removeDefaultGroup(GroupModel group) {
        entity.removeDefaultGroupId(group.getId());
    }

    @Override
    public Stream<ClientModel> getClientsStream() {
        return session.clients().getClientsStream(this);
    }

    @Override
    public Stream<ClientModel> getClientsStream(Integer firstResult, Integer maxResults) {
        return session.clients().getClientsStream(this, firstResult, maxResults);
    }

    @Override
    public Long getClientsCount() {
        return session.clients().getClientsCount(this);
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream() {
        return session.clients().getAlwaysDisplayInConsoleClientsStream(this);
    }

    @Override
    public ClientModel addClient(String name) {
        return session.clients().addClient(this, name);
    }

    @Override
    public ClientModel addClient(String id, String clientId) {
        return session.clients().addClient(this, id, clientId);
    }

    @Override
    public boolean removeClient(String id) {
        return session.clients().removeClient(this, id);
    }

    @Override
    public ClientModel getClientById(String id) {
        return session.clients().getClientById(this, id);
    }

    @Override
    public ClientModel getClientByClientId(String clientId) {
        return session.clients().getClientByClientId(this, clientId);
    }

    @Override
    public Stream<ClientModel> searchClientByClientIdStream(String clientId, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByClientIdStream(this, clientId, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAttributes(
            Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return session.clients().searchClientsByAttributes(this, attributes, firstResult, maxResults);
    }

    @Override
    public Stream<ClientModel> searchClientByAuthenticationFlowBindingOverrides(
            Map<String, String> overrides, Integer firstResult, Integer maxResults) {
        return session.clients()
                .searchClientsByAuthenticationFlowBindingOverrides(this, overrides, firstResult, maxResults);
    }

    @Override
    public Map<String, String> getSmtpConfig() {
        Map<String, String> sC = entity.getSmtpConfig();
        return sC == null ? Collections.emptyMap() : Collections.unmodifiableMap(sC);
    }

    @Override
    public void setSmtpConfig(Map<String, String> smtpConfig) {
        entity.setSmtpConfig(smtpConfig);
    }

    @Override
    public AuthenticationFlowModel getBrowserFlow() {
        return getAuthenticationFlowById(entity.getBrowserFlow());
    }

    @Override
    public void setBrowserFlow(AuthenticationFlowModel flow) {
        entity.setBrowserFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getRegistrationFlow() {
        return getAuthenticationFlowById(entity.getRegistrationFlow());
    }

    @Override
    public void setRegistrationFlow(AuthenticationFlowModel flow) {
        entity.setRegistrationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDirectGrantFlow() {
        return getAuthenticationFlowById(entity.getDirectGrantFlow());
    }

    @Override
    public void setDirectGrantFlow(AuthenticationFlowModel flow) {
        entity.setDirectGrantFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getResetCredentialsFlow() {
        return getAuthenticationFlowById(entity.getResetCredentialsFlow());
    }

    @Override
    public void setResetCredentialsFlow(AuthenticationFlowModel flow) {
        entity.setResetCredentialsFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getClientAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getClientAuthenticationFlow());
    }

    @Override
    public void setClientAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setClientAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getDockerAuthenticationFlow() {
        return getAuthenticationFlowById(entity.getDockerAuthenticationFlow());
    }

    @Override
    public void setDockerAuthenticationFlow(AuthenticationFlowModel flow) {
        entity.setDockerAuthenticationFlow(flow.getId());
    }

    @Override
    public AuthenticationFlowModel getFirstBrokerLoginFlow() {
        return getAuthenticationFlowById(entity.getFirstBrokerLoginFlow());
    }

    @Override
    public void setFirstBrokerLoginFlow(AuthenticationFlowModel authenticationFlowModel) {
        entity.setFirstBrokerLoginFlow(authenticationFlowModel.getId());
    }

    @Override
    public Stream<AuthenticationFlowModel> getAuthenticationFlowsStream() {
        Set<FileAuthenticationFlowEntity> afs = new HashSet<>(entity.getAuthenticationFlows());
        return afs == null ? Stream.empty() : afs.stream().map(FileAuthenticationFlowEntity::toModel);
    }

    @Override
    public AuthenticationFlowModel getFlowByAlias(String alias) {
        Set<FileAuthenticationFlowEntity> afs = new HashSet<>(entity.getAuthenticationFlows());
        return afs == null
                ? null
                : afs.stream()
                        .filter(flow -> Objects.equals(flow.getAlias(), alias))
                        .findFirst()
                        .map(FileAuthenticationFlowEntity::toModel)
                        .orElse(null);
    }

    @Override
    public AuthenticationFlowModel addAuthenticationFlow(AuthenticationFlowModel model) {
        if (entity.getAuthenticationFlow(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An AuthenticationFlow with given id already exists");
        }
        FileAuthenticationFlowEntity authenticationFlowEntity = FileAuthenticationFlowEntity.fromModel(model);
        entity.addAuthenticationFlow(authenticationFlowEntity);
        return FileAuthenticationFlowEntity.toModel(authenticationFlowEntity);
    }

    @Override
    public AuthenticationFlowModel getAuthenticationFlowById(String flowId) {
        if (flowId == null) return null;
        return entity.getAuthenticationFlow(flowId)
                .map(FileAuthenticationFlowEntity::toModel)
                .orElse(null);
    }

    @Override
    public void removeAuthenticationFlow(AuthenticationFlowModel model) {
        entity.removeAuthenticationFlow(model.getId());
    }

    @Override
    public void updateAuthenticationFlow(AuthenticationFlowModel model) {
        entity.getAuthenticationFlow(model.getId()).ifPresent(existing -> {
            existing.setAlias(model.getAlias());
            existing.setDescription(model.getDescription());
            existing.setProviderId(model.getProviderId());
            existing.setBuiltIn(model.isBuiltIn());
            existing.setTopLevel(model.isTopLevel());
        });
    }

    @Override
    public Stream<AuthenticationExecutionModel> getAuthenticationExecutionsStream(String flowId) {
        Set<FileAuthenticationExecutionEntity> aee = new HashSet<>(entity.getAuthenticationExecutions());
        return aee == null
                ? Stream.empty()
                : aee.stream()
                        .filter(execution -> Objects.equals(flowId, execution.getParentFlowId()))
                        .map(FileAuthenticationExecutionEntity::toModel)
                        .sorted(AuthenticationExecutionModel.ExecutionComparator.SINGLETON);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionById(String id) {
        if (id == null) return null;
        return entity.getAuthenticationExecution(id)
                .map(FileAuthenticationExecutionEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticationExecutionModel getAuthenticationExecutionByFlowId(String flowId) {
        Set<FileAuthenticationExecutionEntity> aee = new HashSet<>(entity.getAuthenticationExecutions());
        return aee == null
                ? null
                : aee.stream()
                        .filter(execution -> Objects.equals(flowId, execution.getFlowId()))
                        .findAny()
                        .map(FileAuthenticationExecutionEntity::toModel)
                        .orElse(null);
    }

    @Override
    public AuthenticationExecutionModel addAuthenticatorExecution(AuthenticationExecutionModel model) {
        if (entity.getAuthenticationExecution(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An RequiredActionProvider with given id already exists");
        }
        FileAuthenticationExecutionEntity executionEntity = FileAuthenticationExecutionEntity.fromModel(model);
        entity.addAuthenticationExecution(executionEntity);
        return FileAuthenticationExecutionEntity.toModel(executionEntity);
    }

    @Override
    public void updateAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.getAuthenticationExecution(model.getId()).ifPresent(existing -> {
            existing.setAuthenticator(model.getAuthenticator());
            existing.setAuthenticatorConfig(model.getAuthenticatorConfig());
            existing.setFlowId(model.getFlowId());
            existing.setParentFlowId(model.getParentFlow());
            existing.setRequirement(model.getRequirement());
            existing.setAutheticatorFlow(model.isAuthenticatorFlow());
            existing.setPriority(model.getPriority());
        });
    }

    @Override
    public void removeAuthenticatorExecution(AuthenticationExecutionModel model) {
        entity.removeAuthenticationExecution(model.getId());
    }

    @Override
    public Stream<AuthenticatorConfigModel> getAuthenticatorConfigsStream() {
        Set<FileAuthenticatorConfigEntity> acs = new HashSet<>(entity.getAuthenticatorConfigs());
        return acs == null ? Stream.empty() : acs.stream().map(FileAuthenticatorConfigEntity::toModel);
    }

    @Override
    public AuthenticatorConfigModel addAuthenticatorConfig(AuthenticatorConfigModel model) {
        if (entity.getAuthenticatorConfig(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An Authenticator Config with given id already exists.");
        }
        FileAuthenticatorConfigEntity authenticatorConfig = FileAuthenticatorConfigEntity.fromModel(model);
        entity.addAuthenticatorConfig(authenticatorConfig);
        model.setId(authenticatorConfig.getId());
        return model;
    }

    @Override
    public void updateAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.getAuthenticatorConfig(model.getId()).ifPresent(oldAC -> {
            oldAC.setAlias(model.getAlias());
            oldAC.setConfig(model.getConfig());
        });
    }

    @Override
    public void removeAuthenticatorConfig(AuthenticatorConfigModel model) {
        entity.removeAuthenticatorConfig(model.getId());
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigById(String id) {
        if (id == null) return null;
        return entity.getAuthenticatorConfig(id)
                .map(FileAuthenticatorConfigEntity::toModel)
                .orElse(null);
    }

    @Override
    public AuthenticatorConfigModel getAuthenticatorConfigByAlias(String alias) {
        Set<FileAuthenticatorConfigEntity> acs = new HashSet<>(entity.getAuthenticatorConfigs());
        return acs == null
                ? null
                : acs.stream()
                        .filter(config -> Objects.equals(config.getAlias(), alias))
                        .findFirst()
                        .map(FileAuthenticatorConfigEntity::toModel)
                        .orElse(null);
    }

    @Override
    public RequiredActionConfigModel getRequiredActionConfigById(String id) {
        if (id == null) return null;
        return entity.getRequiredActionConfig(id)
                .map(FileRequiredActionConfigEntity::toModel)
                .orElse(null);
    }

    @Override
    public RequiredActionConfigModel getRequiredActionConfigByAlias(String alias) {
        Set<FileRequiredActionConfigEntity> acs = new HashSet<>(entity.getRequiredActionConfigs());
        return acs == null
                ? null
                : acs.stream()
                        .filter(config -> Objects.equals(config.getAlias(), alias))
                        .findFirst()
                        .map(FileRequiredActionConfigEntity::toModel)
                        .orElse(null);
    }

    @Override
    public void removeRequiredActionProviderConfig(RequiredActionConfigModel model) {
        entity.removeRequiredActionConfig(model.getId());
    }

    @Override
    public void updateRequiredActionConfig(RequiredActionConfigModel model) {
        entity.getRequiredActionConfig(model.getId()).ifPresent(oldAC -> {
            oldAC.setAlias(model.getAlias());
            oldAC.setProviderId(model.getProviderId());
            oldAC.setConfig(model.getConfig());
        });
    }

    @Override
    public Stream<RequiredActionConfigModel> getRequiredActionConfigsStream() {
        Set<FileRequiredActionConfigEntity> acs = new HashSet<>(entity.getRequiredActionConfigs());
        return acs == null ? Stream.empty() : acs.stream().map(FileRequiredActionConfigEntity::toModel);
    }

    @Override
    public Stream<RequiredActionProviderModel> getRequiredActionProvidersStream() {
        Set<FileRequiredActionProviderEntity> raps = new HashSet<>(entity.getRequiredActionProviders());
        return raps == null
                ? Stream.empty()
                : raps.stream()
                        .map(FileRequiredActionProviderEntity::toModel)
                        .sorted(RequiredActionProviderModel.RequiredActionComparator.SINGLETON);
    }

    @Override
    public RequiredActionProviderModel addRequiredActionProvider(RequiredActionProviderModel model) {
        if (entity.getRequiredActionProvider(model.getId()).isPresent()) {
            throw new ModelDuplicateException("A Required Action Provider with given id already exists.");
        }
        if (getRequiredActionProviderByAlias(model.getAlias()) != null) {
            throw new ModelDuplicateException("A Required Action Provider with given alias already exists.");
        }
        FileRequiredActionProviderEntity requiredActionProvider = FileRequiredActionProviderEntity.fromModel(model);
        entity.addRequiredActionProvider(requiredActionProvider);
        return FileRequiredActionProviderEntity.toModel(requiredActionProvider);
    }

    @Override
    public void updateRequiredActionProvider(RequiredActionProviderModel model) {
        entity.getRequiredActionProvider(model.getId()).ifPresent(oldRAP -> {
            oldRAP.setAlias(model.getAlias());
            oldRAP.setName(model.getName());
            oldRAP.setProviderId(model.getProviderId());
            oldRAP.setPriority(model.getPriority());
            oldRAP.setEnabled(model.isEnabled());
            oldRAP.setDefaultAction(model.isDefaultAction());
            oldRAP.setConfig(model.getConfig());
        });
    }

    @Override
    public void removeRequiredActionProvider(RequiredActionProviderModel model) {
        entity.removeRequiredActionProvider(model.getId());
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderById(String id) {
        if (id == null) return null;
        return entity.getRequiredActionProvider(id)
                .map(FileRequiredActionProviderEntity::toModel)
                .orElse(null);
    }

    @Override
    public RequiredActionProviderModel getRequiredActionProviderByAlias(String alias) {
        Set<FileRequiredActionProviderEntity> raps = new HashSet<>(entity.getRequiredActionProviders());
        return raps == null
                ? null
                : raps.stream()
                        .filter(actionProvider -> Objects.equals(actionProvider.getAlias(), alias))
                        .findFirst()
                        .map(FileRequiredActionProviderEntity::toModel)
                        .orElse(null);
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProvidersStream() {
        Set<FileIdentityProviderEntity> ips = new HashSet<>(entity.getIdentityProviders());
        return ips == null
                ? Stream.empty()
                : ips.stream()
                        .map(e -> FileIdentityProviderEntity.toModel(
                                e, () -> this.getModelFromProviderFactory(e.getProviderId())));
    }

    @Override
    public IdentityProviderModel getIdentityProviderByAlias(String alias) {
        Set<FileIdentityProviderEntity> ips = new HashSet<>(entity.getIdentityProviders());
        return ips == null
                ? null
                : ips.stream()
                        .filter(identityProvider -> Objects.equals(identityProvider.getAlias(), alias))
                        .findFirst()
                        .map(e -> FileIdentityProviderEntity.toModel(
                                e, () -> this.getModelFromProviderFactory(e.getProviderId())))
                        .orElse(null);
    }

    // This is a violation of layering requirements, this should NOT be in store code.
    // However, there is no easy way around this given the current number of IdentityProviderModel
    // implementations
    private IdentityProviderModel getModelFromProviderFactory(String providerId) {
        Optional<IdentityProviderFactory> factory = Stream.concat(
                        session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                        session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerFactory.getId(), providerId))
                .map(IdentityProviderFactory.class::cast)
                .findFirst();
        if (factory.isPresent()) {
            return factory.get().createConfig();
        } else {
            LOG.warn("Couldn't find a suitable identity provider factory for " + providerId);
            return new IdentityProviderModel();
        }
    }

    @Override
    public void addIdentityProvider(IdentityProviderModel model) {
        if (getIdentityProviderByAlias(model.getAlias()) != null) {
            throw new ModelDuplicateException("An Identity Provider with given alias already exists.");
        }
        entity.addIdentityProvider(FileIdentityProviderEntity.fromModel(model));
    }

    @Override
    public void removeIdentityProviderByAlias(String alias) {
        IdentityProviderModel model = getIdentityProviderByAlias(alias);

        if (model == null) {
            return;
        }

        entity.removeIdentityProvider(model.getInternalId());
        session.getKeycloakSessionFactory().publish(new IdentityProviderRemovedEvent() {

            @Override
            public RealmModel getRealm() {
                return FileRealmAdapter.this;
            }

            @Override
            public IdentityProviderModel getRemovedIdentityProvider() {
                return model;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    @Override
    public void updateIdentityProvider(IdentityProviderModel identityProvider) {
        Set<FileIdentityProviderEntity> ips = new HashSet<>(entity.getIdentityProviders());
        if (ips != null) {
            ips.stream()
                    .filter(ip -> Objects.equals(ip.getId(), identityProvider.getInternalId()))
                    .findFirst()
                    .ifPresent(oldPS -> {
                        oldPS.setAlias(identityProvider.getAlias());
                        oldPS.setDisplayName(identityProvider.getDisplayName());
                        oldPS.setProviderId(identityProvider.getProviderId());
                        oldPS.setFirstBrokerLoginFlowId(identityProvider.getFirstBrokerLoginFlowId());
                        oldPS.setPostBrokerLoginFlowId(identityProvider.getPostBrokerLoginFlowId());
                        oldPS.setEnabled(identityProvider.isEnabled());
                        oldPS.setTrustEmail(identityProvider.isTrustEmail());
                        oldPS.setStoreToken(identityProvider.isStoreToken());
                        oldPS.setLinkOnly(identityProvider.isLinkOnly());
                        oldPS.setAddReadTokenRoleOnCreate(identityProvider.isAddReadTokenRoleOnCreate());
                        oldPS.setAuthenticateByDefault(identityProvider.isAuthenticateByDefault());
                        oldPS.setHideOnLogin(identityProvider.isHideOnLogin());
                        oldPS.setConfig(
                                identityProvider.getConfig() == null
                                        ? null
                                        : new HashMap<>(identityProvider.getConfig()));
                    });
            session.getKeycloakSessionFactory().publish(new IdentityProviderUpdatedEvent() {

                @Override
                public RealmModel getRealm() {
                    return FileRealmAdapter.this;
                }

                @Override
                public IdentityProviderModel getUpdatedIdentityProvider() {
                    return identityProvider;
                }

                @Override
                public KeycloakSession getKeycloakSession() {
                    return session;
                }
            });
        }
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersStream() {
        Set<FileIdentityProviderMapperEntity> ipms = new HashSet<>(entity.getIdentityProviderMappers());
        return ipms == null ? Stream.empty() : ipms.stream().map(FileIdentityProviderMapperEntity::toModel);
    }

    @Override
    public Stream<IdentityProviderMapperModel> getIdentityProviderMappersByAliasStream(String brokerAlias) {
        Set<FileIdentityProviderMapperEntity> ipms = new HashSet<>(entity.getIdentityProviderMappers());
        return ipms == null
                ? Stream.empty()
                : ipms.stream()
                        .filter(mapper -> Objects.equals(mapper.getIdentityProviderAlias(), brokerAlias))
                        .map(FileIdentityProviderMapperEntity::toModel);
    }

    @Override
    public IdentityProviderMapperModel addIdentityProviderMapper(IdentityProviderMapperModel model) {
        FileIdentityProviderMapperEntity identityProviderMapper = FileIdentityProviderMapperEntity.fromModel(model);
        if (entity.getIdentityProviderMapper(model.getId()).isPresent()) {
            throw new ModelDuplicateException("An IdentityProviderMapper with given id already exists");
        }
        entity.addIdentityProviderMapper(identityProviderMapper);
        return FileIdentityProviderMapperEntity.toModel(identityProviderMapper);
    }

    @Override
    public void removeIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.removeIdentityProviderMapper(model.getId());
    }

    @Override
    public void updateIdentityProviderMapper(IdentityProviderMapperModel model) {
        entity.getIdentityProviderMapper(model.getId()).ifPresent(oldIPM -> {
            oldIPM.setName(model.getName());
            oldIPM.setIdentityProviderAlias(model.getIdentityProviderAlias());
            oldIPM.setIdentityProviderMapper(model.getIdentityProviderMapper());
            oldIPM.setConfig(model.getConfig());
        });
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperById(String id) {
        if (id == null) return null;
        return entity.getIdentityProviderMapper(id)
                .map(FileIdentityProviderMapperEntity::toModel)
                .orElse(null);
    }

    @Override
    public IdentityProviderMapperModel getIdentityProviderMapperByName(String brokerAlias, String name) {
        Set<FileIdentityProviderMapperEntity> ipms = new HashSet<>(entity.getIdentityProviderMappers());
        return ipms == null
                ? null
                : ipms.stream()
                        .filter(identityProviderMapper ->
                                Objects.equals(identityProviderMapper.getIdentityProviderAlias(), brokerAlias)
                                        && Objects.equals(identityProviderMapper.getName(), name))
                        .findFirst()
                        .map(FileIdentityProviderMapperEntity::toModel)
                        .orElse(null);
    }

    @Override
    public ComponentModel addComponentModel(ComponentModel model) {
        model = importComponentModel(model);
        ComponentUtil.notifyCreated(session, this, model);
        return model;
    }

    /** Copied from jpa RealmAdapter: This just exists for testing purposes */
    private static final String COMPONENT_PROVIDER_EXISTS_DISABLED = "component.provider.exists.disabled";

    @Override
    public ComponentModel importComponentModel(ComponentModel model) {
        try {
            ComponentFactory componentFactory = ComponentUtil.getComponentFactory(session, model);
            if (componentFactory == null && System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw new IllegalArgumentException("Invalid component type");
            }
            componentFactory.validateConfiguration(session, this, model);
        } catch (IllegalArgumentException | ComponentValidationException e) {
            if (System.getProperty(COMPONENT_PROVIDER_EXISTS_DISABLED) == null) {
                throw e;
            }
        }
        if (model.getId() != null && entity.getComponent(model.getId()).isPresent()) {
            LOG.warn("Removing existing component with ID " + model.getId() + " since it already exists.");
            entity.removeComponent(model.getId());
        }
        FileComponentEntity component = FileComponentEntity.fromModel(model);
        if (model.getParentId() == null) {
            component.setParentId(getId());
        }
        entity.addComponent(component);
        return FileComponentEntity.toModel(component);
    }

    @Override
    public void updateComponent(ComponentModel component) {
        ComponentUtil.getComponentFactory(session, component).validateConfiguration(session, this, component);
        entity.getComponent(component.getId()).ifPresent(existing -> {
            ComponentModel oldModel = FileComponentEntity.toModel(existing);
            updateComponent(existing, component);
            ComponentUtil.notifyUpdated(session, this, oldModel, component);
        });
    }

    private static void updateComponent(FileComponentEntity oldValue, ComponentModel newValue) {
        oldValue.setName(newValue.getName());
        oldValue.setProviderId(newValue.getProviderId());
        oldValue.setProviderType(newValue.getProviderType());
        oldValue.setSubType(newValue.getSubType());
        oldValue.setParentId(newValue.getParentId());
        oldValue.convertFromMultivaluedConfig(newValue.getConfig());
    }

    @Override
    public void removeComponent(ComponentModel component) {
        if (!entity.getComponent(component.getId()).isPresent()) return;
        session.users().preRemove(this, component);
        ComponentUtil.notifyPreRemove(session, this, component);
        removeComponents(component.getId());
        entity.removeComponent(component.getId());
    }

    @Override
    public void removeComponents(String parentId) {
        Set<FileComponentEntity> components = new HashSet<>(entity.getComponents());
        if (components == null || components.isEmpty()) return;
        components.stream()
                .filter(c -> Objects.equals(parentId, c.getParentId()))
                .map(FileComponentEntity::toModel)
                .collect(Collectors.toSet()) // This is necessary to read out all the components before removing them
                .forEach(c -> {
                    session.users().preRemove(this, c);
                    ComponentUtil.notifyPreRemove(session, this, c);
                    entity.removeComponent(c.getId());
                });
    }

    @Override
    public Stream<ComponentModel> getComponentsStream() {
        Set<FileComponentEntity> components = new HashSet<>(entity.getComponents());
        return components == null ? Stream.empty() : components.stream().map(FileComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId) {
        Set<FileComponentEntity> components = new HashSet<>(entity.getComponents());
        return components == null
                ? Stream.empty()
                : components.stream()
                        .filter(c -> Objects.equals(parentId, c.getParentId()))
                        .map(FileComponentEntity::toModel);
    }

    @Override
    public Stream<ComponentModel> getComponentsStream(String parentId, String providerType) {
        Set<FileComponentEntity> components = new HashSet<>(entity.getComponents());
        return components == null
                ? Stream.empty()
                : components.stream()
                        .filter(c -> Objects.equals(parentId, c.getParentId()))
                        .filter(c -> Objects.equals(providerType, c.getProviderType()))
                        .map(FileComponentEntity::toModel);
    }

    @Override
    public ComponentModel getComponent(String id) {
        return entity.getComponent(id).map(FileComponentEntity::toModel).orElse(null);
    }

    @Override
    public String getLoginTheme() {
        return entity.getLoginTheme();
    }

    @Override
    public void setLoginTheme(String name) {
        entity.setLoginTheme(name);
    }

    @Override
    public String getAccountTheme() {
        return entity.getAccountTheme();
    }

    @Override
    public void setAccountTheme(String name) {
        entity.setAccountTheme(name);
    }

    @Override
    public String getAdminTheme() {
        return entity.getAdminTheme();
    }

    @Override
    public void setAdminTheme(String name) {
        entity.setAdminTheme(name);
    }

    @Override
    public String getEmailTheme() {
        return entity.getEmailTheme();
    }

    @Override
    public void setEmailTheme(String name) {
        entity.setEmailTheme(name);
    }

    @Override
    public int getNotBefore() {
        Long notBefore = entity.getNotBefore();
        return notBefore == null ? 0 : TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(notBefore);
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(TimeAdapter.fromIntegerWithTimeInSecondsToLongWithTimeAsInSeconds(notBefore));
    }

    @Override
    public boolean isEventsEnabled() {
        Boolean is = entity.isEventsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setEventsEnabled(boolean enabled) {
        entity.setEventsEnabled(enabled);
    }

    @Override
    public long getEventsExpiration() {
        Long i = entity.getEventsExpiration();
        return i == null ? 0 : i;
    }

    @Override
    public void setEventsExpiration(long expiration) {
        entity.setEventsExpiration(expiration);
    }

    @Override
    public Stream<String> getEventsListenersStream() {
        Set<String> eLs = new HashSet<>(entity.getEventsListeners());
        return eLs == null ? Stream.empty() : eLs.stream();
    }

    @Override
    public void setEventsListeners(Set<String> listeners) {
        entity.setEventsListeners(new ArrayList<>(listeners.stream().toList()));
    }

    @Override
    public Stream<String> getEnabledEventTypesStream() {
        Set<String> eETs = new HashSet<>(entity.getEnabledEventTypes());
        return eETs == null ? Stream.empty() : eETs.stream();
    }

    @Override
    public void setEnabledEventTypes(Set<String> enabledEventTypes) {
        entity.setEnabledEventTypes(new ArrayList<>(enabledEventTypes.stream().toList()));
    }

    @Override
    public boolean isAdminEventsEnabled() {
        Boolean is = entity.isAdminEventsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setAdminEventsEnabled(boolean enabled) {
        entity.setAdminEventsEnabled(enabled);
    }

    @Override
    public boolean isAdminEventsDetailsEnabled() {
        Boolean is = entity.isAdminEventsDetailsEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setAdminEventsDetailsEnabled(boolean enabled) {
        entity.setAdminEventsDetailsEnabled(enabled);
    }

    @Override
    public ClientModel getMasterAdminClient() {
        String masterAdminClientId = entity.getMasterAdminClient();
        if (masterAdminClientId == null) {
            return null;
        }
        RealmModel masterRealm = getName().equals(Config.getAdminRealm())
                ? this
                : session.realms().getRealmByName(Config.getAdminRealm());
        return session.clients().getClientById(masterRealm, masterAdminClientId);
    }

    @Override
    public void setMasterAdminClient(ClientModel client) {
        String id = client == null ? null : client.getId();
        entity.setMasterAdminClient(id);
    }

    @Override
    public RoleModel getDefaultRole() {
        return session.roles().getRoleById(this, entity.getDefaultRoleId());
    }

    @Override
    public void setDefaultRole(RoleModel role) {
        entity.setDefaultRoleId(role.getId());
    }

    @Override
    public ClientModel getAdminPermissionsClient() {
        if (getAttribute(ADMIN_PERMISSIONS_CLIENT_ID) == null) {
            return null;
        }
        return session.clients().getClientById(this, getAttribute(ADMIN_PERMISSIONS_CLIENT_ID));
    }

    @Override
    public void setAdminPermissionsClient(ClientModel client) {
        setAttribute(ADMIN_PERMISSIONS_CLIENT_ID, client.getId());
    }

    @Override
    public boolean isIdentityFederationEnabled() {
        Set<FileIdentityProviderEntity> ips = new HashSet<>(entity.getIdentityProviders());
        return ips != null && ips.stream().findAny().isPresent();
    }

    @Override
    public boolean isInternationalizationEnabled() {
        Boolean is = entity.isInternationalizationEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setInternationalizationEnabled(boolean enabled) {
        entity.setInternationalizationEnabled(enabled);
    }

    @Override
    public Stream<String> getSupportedLocalesStream() {
        Set<String> sLs = new HashSet<>(entity.getSupportedLocales());
        return sLs == null ? Stream.empty() : sLs.stream();
    }

    @Override
    public void setSupportedLocales(Set<String> locales) {
        entity.setSupportedLocales(new ArrayList<>(locales.stream().toList()));
    }

    @Override
    public String getDefaultLocale() {
        return entity.getDefaultLocale();
    }

    @Override
    public void setDefaultLocale(String locale) {
        entity.setDefaultLocale(locale);
    }

    @Override
    public GroupModel createGroup(String id, String name, GroupModel toParent) {
        return session.groups().createGroup(this, id, name, toParent);
    }

    @Override
    public GroupModel getGroupById(String id) {
        return session.groups().getGroupById(this, id);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return session.groups().getGroupsStream(this);
    }

    @Override
    public Long getGroupsCount(Boolean onlyTopGroups) {
        return session.groups().getGroupsCount(this, onlyTopGroups);
    }

    @Override
    public Long getGroupsCountByNameContaining(String search) {
        return session.groups().getGroupsCountByNameContaining(this, search);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream() {
        return session.groups().getTopLevelGroupsStream(this);
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(Integer first, Integer max) {
        return session.groups().getTopLevelGroupsStream(this, first, max);
    }

    @Override
    public boolean removeGroup(GroupModel group) {
        return session.groups().removeGroup(this, group);
    }

    @Override
    public void moveGroup(GroupModel group, GroupModel toParent) {
        session.groups().moveGroup(this, group, toParent);
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream() {
        return session.clientScopes().getClientScopesStream(this);
    }

    @Override
    public ClientScopeModel addClientScope(String name) {
        return session.clientScopes().addClientScope(this, name);
    }

    @Override
    public ClientScopeModel addClientScope(String id, String name) {
        return session.clientScopes().addClientScope(this, id, name);
    }

    @Override
    public boolean removeClientScope(String id) {
        return session.clientScopes().removeClientScope(this, id);
    }

    @Override
    public ClientScopeModel getClientScopeById(String id) {
        return session.clientScopes().getClientScopeById(this, id);
    }

    @Override
    public void addDefaultClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        if (defaultScope) {
            entity.addDefaultClientScopeId(clientScope.getId());
        } else {
            entity.addOptionalClientScopeId(clientScope.getId());
        }
    }

    @Override
    public void removeDefaultClientScope(ClientScopeModel clientScope) {
        Boolean removedDefault = entity.removeDefaultClientScopeId(clientScope.getId());
        if (removedDefault == null || !removedDefault) {
            entity.removeOptionalClientScopeId(clientScope.getId());
        }
    }

    @Override
    public Stream<ClientScopeModel> getDefaultClientScopesStream(boolean defaultScope) {
        Set<String> csIds = defaultScope
                ? new HashSet<>(entity.getDefaultClientScopeIds())
                : new HashSet<>(entity.getOptionalClientScopeIds());
        return csIds == null ? Stream.empty() : csIds.stream().map(this::getClientScopeById);
    }

    @Override
    public void createOrUpdateRealmLocalizationTexts(String locale, Map<String, String> localizationTexts) {
        Map<String, Map<String, String>> realmLocalizationTexts = entity.getLocalizationTexts();
        if (realmLocalizationTexts != null && realmLocalizationTexts.containsKey(locale)) {
            Map<String, String> currentTexts = new HashMap<>(realmLocalizationTexts.get(locale));
            currentTexts.putAll(localizationTexts);
            entity.setLocalizationText(locale, currentTexts);
        } else {
            entity.setLocalizationText(locale, localizationTexts);
        }
    }

    @Override
    public boolean removeRealmLocalizationTexts(String locale) {
        if (locale == null) return false;
        return entity.removeLocalizationText(locale);
    }

    @Override
    public Map<String, Map<String, String>> getRealmLocalizationTexts() {
        Map<String, Map<String, String>> localizationTexts = entity.getLocalizationTexts();
        return localizationTexts == null ? Collections.emptyMap() : localizationTexts;
    }

    @Override
    public Map<String, String> getRealmLocalizationTextsByLocale(String locale) {
        Map<String, String> lT = entity.getLocalizationText(locale);
        return lT == null ? Collections.emptyMap() : lT;
    }

    @Override
    public RoleModel getRole(String name) {
        return session.roles().getRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.roles().addRealmRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addRealmRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return session.roles().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getRealmRolesStream(this);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return session.roles().getRealmRolesStream(this, firstResult, maxResults);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForRolesStream(this, search, first, max);
    }

    @Override
    public boolean isBruteForceProtected() {
        return getAttribute(BRUTE_FORCE_PROTECTED, false);
    }

    @Override
    public void setBruteForceProtected(boolean value) {
        setAttribute(BRUTE_FORCE_PROTECTED, value);
    }

    @Override
    public boolean isPermanentLockout() {
        return getAttribute(PERMANENT_LOCKOUT, false);
    }

    @Override
    public void setPermanentLockout(final boolean val) {
        setAttribute(PERMANENT_LOCKOUT, val);
    }

    @Override
    public int getMaxTemporaryLockouts() {
        return getAttribute(MAX_TEMPORARY_LOCKOUTS, 0);
    }

    @Override
    public void setMaxTemporaryLockouts(int i) {
        setAttribute(MAX_TEMPORARY_LOCKOUTS, i);
    }

    @Override
    public RealmRepresentation.BruteForceStrategy getBruteForceStrategy() {
        String strategy = getAttribute(BRUTE_FORCE_STRATEGY);
        return strategy == null
                ? RealmRepresentation.BruteForceStrategy.MULTIPLE
                : RealmRepresentation.BruteForceStrategy.valueOf(strategy);
    }

    @Override
    public void setBruteForceStrategy(RealmRepresentation.BruteForceStrategy bruteForceStrategy) {
        setAttribute(BRUTE_FORCE_STRATEGY, bruteForceStrategy == null ? null : bruteForceStrategy.name());
    }

    @Override
    public int getMaxFailureWaitSeconds() {
        return getAttribute(MAX_FAILURE_WAIT_SECONDS, 0);
    }

    @Override
    public void setMaxFailureWaitSeconds(int val) {
        setAttribute(MAX_FAILURE_WAIT_SECONDS, val);
    }

    @Override
    public int getWaitIncrementSeconds() {
        return getAttribute(WAIT_INCREMENT_SECONDS, 0);
    }

    @Override
    public void setWaitIncrementSeconds(int val) {
        setAttribute(WAIT_INCREMENT_SECONDS, val);
    }

    @Override
    public int getMinimumQuickLoginWaitSeconds() {
        return getAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, 0);
    }

    @Override
    public void setMinimumQuickLoginWaitSeconds(int val) {
        setAttribute(MINIMUM_QUICK_LOGIN_WAIT_SECONDS, val);
    }

    @Override
    public long getQuickLoginCheckMilliSeconds() {
        return getAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, 0L);
    }

    @Override
    public void setQuickLoginCheckMilliSeconds(long val) {
        setAttribute(QUICK_LOGIN_CHECK_MILLISECONDS, val);
    }

    @Override
    public int getMaxDeltaTimeSeconds() {
        return getAttribute(MAX_DELTA_SECONDS, 0);
    }

    @Override
    public void setMaxDeltaTimeSeconds(int val) {
        setAttribute(MAX_DELTA_SECONDS, val);
    }

    @Override
    public int getFailureFactor() {
        return getAttribute(FAILURE_FACTOR, 0);
    }

    @Override
    public void setFailureFactor(int failureFactor) {
        setAttribute(FAILURE_FACTOR, failureFactor);
    }

    @Override
    public String getDefaultSignatureAlgorithm() {
        return getAttribute(DEFAULT_SIGNATURE_ALGORITHM);
    }

    @Override
    public void setDefaultSignatureAlgorithm(String defaultSignatureAlgorithm) {
        setAttribute(DEFAULT_SIGNATURE_ALGORITHM, defaultSignatureAlgorithm);
    }

    @Override
    public boolean isOfflineSessionMaxLifespanEnabled() {
        Boolean is = entity.isOfflineSessionMaxLifespanEnabled();
        return is == null ? false : is;
    }

    @Override
    public void setOfflineSessionMaxLifespanEnabled(boolean offlineSessionMaxLifespanEnabled) {
        entity.setOfflineSessionMaxLifespanEnabled(offlineSessionMaxLifespanEnabled);
    }

    @Override
    public int getOfflineSessionMaxLifespan() {
        Integer i = entity.getOfflineSessionMaxLifespan();
        return i == null ? 0 : i;
    }

    @Override
    public void setOfflineSessionMaxLifespan(int seconds) {
        entity.setOfflineSessionMaxLifespan(seconds);
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicy() {
        FileWebAuthnPolicyEntity policy = entity.getWebAuthnPolicy();
        if (policy == null) policy = FileWebAuthnPolicyEntity.defaultWebAuthnPolicy();
        return FileWebAuthnPolicyEntity.toModel(policy);
    }

    @Override
    public void setWebAuthnPolicy(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicy(FileWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public WebAuthnPolicy getWebAuthnPolicyPasswordless() {
        FileWebAuthnPolicyEntity policy = entity.getWebAuthnPolicyPasswordless();
        if (policy == null) policy = FileWebAuthnPolicyEntity.defaultWebAuthnPolicy();
        return FileWebAuthnPolicyEntity.toModel(policy);
    }

    @Override
    public void setWebAuthnPolicyPasswordless(WebAuthnPolicy policy) {
        entity.setWebAuthnPolicyPasswordless(FileWebAuthnPolicyEntity.fromModel(policy));
    }

    @Override
    public Map<String, String> getBrowserSecurityHeaders() {
        Map<String, String> bSH = entity.getBrowserSecurityHeaders();
        return bSH == null ? Collections.emptyMap() : Collections.unmodifiableMap(bSH);
    }

    @Override
    public void setBrowserSecurityHeaders(Map<String, String> headers) {
        entity.setBrowserSecurityHeaders(headers);
    }

    @Override
    public ClientInitialAccessModel createClientInitialAccessModel(int expiration, int count) {
        FileClientInitialAccessEntity clientInitialAccess =
                FileClientInitialAccessEntity.createEntity(expiration, count);
        entity.addClientInitialAccess(clientInitialAccess);
        return FileClientInitialAccessEntity.toModel(clientInitialAccess);
    }

    @Override
    public ClientInitialAccessModel getClientInitialAccessModel(String id) {
        return entity.getClientInitialAccess(id)
                .map(FileClientInitialAccessEntity::toModel)
                .orElse(null);
    }

    @Override
    public void removeClientInitialAccessModel(String id) {
        entity.removeClientInitialAccess(id);
    }

    @Override
    public Stream<ClientInitialAccessModel> getClientInitialAccesses() {
        Set<FileClientInitialAccessEntity> cias = new HashSet<>(entity.getClientInitialAccesses());
        return cias == null ? Stream.empty() : cias.stream().map(FileClientInitialAccessEntity::toModel);
    }

    @Override
    public void decreaseRemainingCount(ClientInitialAccessModel model) {
        entity.getClientInitialAccess(model.getId())
                .ifPresent(cia -> cia.setRemainingCount(model.getRemainingCount() - 1));
    }

    @Override
    public OAuth2DeviceConfig getOAuth2DeviceConfig() {
        return new OAuth2DeviceConfig(this);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }

    @Override
    public CibaConfig getCibaPolicy() {
        return new CibaConfig(this);
    }

    @Override
    public ParConfig getParPolicy() {
        return new ParConfig(this);
    }

    @Override
    public void setScimApiEnabled(boolean enabled) {
        setAttribute(SCIM_API_ENABLED, enabled);
    }

    @Override
    public boolean isScimApiEnabled() {
        return getAttribute(SCIM_API_ENABLED, Boolean.FALSE);
    }

    @Override
    public int getMaxSecondaryAuthFailures() {
        return getAttribute(MAX_SECONDARY_AUTH_FAILURES, 0);
    }

    @Override
    public void setMaxSecondaryAuthFailures(int maxSecondaryAuthFailures) {
        setAttribute(MAX_SECONDARY_AUTH_FAILURES, maxSecondaryAuthFailures);
    }
}
