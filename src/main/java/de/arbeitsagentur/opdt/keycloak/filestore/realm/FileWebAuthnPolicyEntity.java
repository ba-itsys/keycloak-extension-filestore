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

import de.arbeitsagentur.opdt.keycloak.filestore.common.UpdatableEntity;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.keycloak.models.Constants;
import org.keycloak.models.WebAuthnPolicy;

public class FileWebAuthnPolicyEntity implements UpdatableEntity {

    private boolean isUpdated = false;
    private String rpEntityName;
    private List<String> signatureAlgorithms;
    private String rpId;
    private String attestationConveyancePreference;
    private String authenticatorAttachment;
    private String requireResidentKey;
    private String residentKey;
    private String userVerificationRequirement;
    private Integer createTimeout;
    private Boolean avoidSameAuthenticatorRegister = false;
    private List<String> acceptableAaguids;
    private List<String> extraOrigins;

    static FileWebAuthnPolicyEntity fromModel(WebAuthnPolicy model) {
        if (model == null) return null;
        FileWebAuthnPolicyEntity entity = new FileWebAuthnPolicyEntity();
        entity.setRpEntityName(model.getRpEntityName());
        entity.setSignatureAlgorithms(model.getSignatureAlgorithm());
        entity.setRpId(model.getRpId());
        entity.setAttestationConveyancePreference(model.getAttestationConveyancePreference());
        entity.setAuthenticatorAttachment(model.getAuthenticatorAttachment());
        entity.setRequireResidentKey(model.getRequireResidentKey());
        entity.setResidentKey(model.getResidentKey());
        entity.setUserVerificationRequirement(model.getUserVerificationRequirement());
        entity.setCreateTimeout(model.getCreateTimeout());
        entity.setAvoidSameAuthenticatorRegister(model.isAvoidSameAuthenticatorRegister());
        entity.setAcceptableAaguids(model.getAcceptableAaguids());
        entity.setExtraOrigins(model.getExtraOrigins());
        return entity;
    }

    static WebAuthnPolicy toModel(FileWebAuthnPolicyEntity entity) {
        if (entity == null) return null;
        WebAuthnPolicy model = new WebAuthnPolicy();
        model.setRpEntityName(entity.getRpEntityName());
        model.setSignatureAlgorithm(entity.getSignatureAlgorithms());
        model.setRpId(entity.getRpId());
        model.setAttestationConveyancePreference(entity.getAttestationConveyancePreference());
        model.setAuthenticatorAttachment(entity.getAuthenticatorAttachment());
        model.setRequireResidentKey(entity.getRequireResidentKey());
        model.setResidentKey(entity.getResidentKey());
        model.setUserVerificationRequirement(entity.getUserVerificationRequirement());
        model.setCreateTimeout(entity.getCreateTimeout());
        model.setAvoidSameAuthenticatorRegister(entity.isAvoidSameAuthenticatorRegister());
        List<String> acceptableAaguids = entity.getAcceptableAaguids();
        model.setAcceptableAaguids(
                acceptableAaguids == null ? new LinkedList<>() : new LinkedList<>(acceptableAaguids));
        List<String> extraOrigins = entity.getExtraOrigins();
        model.setExtraOrigins(extraOrigins == null ? new LinkedList<>() : new LinkedList<>(extraOrigins));
        return model;
    }

    static FileWebAuthnPolicyEntity defaultWebAuthnPolicy() {
        FileWebAuthnPolicyEntity entity = new FileWebAuthnPolicyEntity();
        entity.setRpEntityName(Constants.DEFAULT_WEBAUTHN_POLICY_RP_ENTITY_NAME);
        entity.setSignatureAlgorithms(Arrays.asList(Constants.DEFAULT_WEBAUTHN_POLICY_SIGNATURE_ALGORITHMS.split(",")));
        entity.setRpId("");
        entity.setAttestationConveyancePreference(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setAuthenticatorAttachment(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setRequireResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setUserVerificationRequirement(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        entity.setCreateTimeout(0);
        entity.setAvoidSameAuthenticatorRegister(false);
        entity.setAcceptableAaguids(new LinkedList<>());
        entity.setExtraOrigins(new LinkedList<>());
        return entity;
    }

    public boolean isUpdated() {
        return this.isUpdated;
    }

    public String getRpEntityName() {
        return this.rpEntityName;
    }

    public void setRpEntityName(String rpEntityName) {
        this.rpEntityName = rpEntityName;
    }

    public List<String> getSignatureAlgorithms() {
        return this.signatureAlgorithms;
    }

    public void setSignatureAlgorithms(List<String> signatureAlgorithms) {
        this.signatureAlgorithms = signatureAlgorithms;
    }

    public String getRpId() {
        return this.rpId;
    }

    public void setRpId(String rpId) {
        this.rpId = rpId;
    }

    public String getAttestationConveyancePreference() {
        return this.attestationConveyancePreference;
    }

    public void setAttestationConveyancePreference(String attestationConveyancePreference) {
        this.attestationConveyancePreference = attestationConveyancePreference;
    }

    public String getAuthenticatorAttachment() {
        return this.authenticatorAttachment;
    }

    public void setAuthenticatorAttachment(String authenticatorAttachment) {
        this.authenticatorAttachment = authenticatorAttachment;
    }

    public String getRequireResidentKey() {
        return this.requireResidentKey;
    }

    public void setRequireResidentKey(String requireResidentKey) {
        this.requireResidentKey = requireResidentKey;
    }

    public String getResidentKey() {
        return this.residentKey;
    }

    public void setResidentKey(String residentKey) {
        this.residentKey = residentKey;
    }

    public String getUserVerificationRequirement() {
        return this.userVerificationRequirement;
    }

    public void setUserVerificationRequirement(String userVerificationRequirement) {
        this.userVerificationRequirement = userVerificationRequirement;
    }

    public Integer getCreateTimeout() {
        return this.createTimeout;
    }

    public void setCreateTimeout(Integer createTimeout) {
        this.createTimeout = createTimeout;
    }

    public Boolean isAvoidSameAuthenticatorRegister() {
        return this.avoidSameAuthenticatorRegister;
    }

    public void setAvoidSameAuthenticatorRegister(Boolean avoidSameAuthenticatorRegister) {
        this.avoidSameAuthenticatorRegister = avoidSameAuthenticatorRegister;
    }

    public List<String> getAcceptableAaguids() {
        return this.acceptableAaguids;
    }

    public void setAcceptableAaguids(List<String> acceptableAaguids) {
        this.acceptableAaguids = acceptableAaguids;
    }

    public List<String> getExtraOrigins() {
        return this.extraOrigins;
    }

    public void setExtraOrigins(List<String> extraOrigins) {
        this.extraOrigins = extraOrigins;
    }

    public void setUpdated(boolean updated) {
        isUpdated = updated;
    }

    public Boolean getAvoidSameAuthenticatorRegister() {
        return avoidSameAuthenticatorRegister;
    }
}
