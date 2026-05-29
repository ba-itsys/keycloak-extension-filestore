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

import static org.keycloak.models.utils.KeycloakModelUtils.runJobInTransaction;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public abstract class KeycloakModelTest {

    public static final String TEST_FILESTORE_DIR =
            "target/test-filestore/" + System.getProperty("surefire.forkNumber", "0");
    public static final String FIXTURE_FILESTORE_DIR = "src/test/filestore";
    public static final String FIXTURE_TEST_FILESTORE_DIR =
            "target/test-fixture-filestore/" + System.getProperty("surefire.forkNumber", "0");

    protected static Stream<String> nullAndEmptyStrings() {
        return Stream.of(null, "");
    }

    protected static Stream<String> nullEmptyAndUnknownStrings() {
        return Stream.of(null, "", "unknown");
    }

    protected static <T, R> R inCommittedTransaction(
            KeycloakSession serverSession, T parameter, BiFunction<KeycloakSession, T, R> what) {
        return inCommittedTransaction(serverSession.getKeycloakSessionFactory(), parameter, what, null);
    }

    protected static void inCommittedTransaction(KeycloakSession serverSession, Consumer<KeycloakSession> what) {
        inCommittedTransaction(serverSession, session -> {
            what.accept(session);
            return null;
        });
    }

    protected static <R> R inCommittedTransaction(KeycloakSession serverSession, Function<KeycloakSession, R> what) {
        return inCommittedTransaction(serverSession, 1, (session, ignored) -> what.apply(session), null);
    }

    protected static <T, R> R inCommittedTransaction(
            KeycloakSession serverSession,
            T parameter,
            BiFunction<KeycloakSession, T, R> what,
            BiConsumer<KeycloakSession, T> onCommit) {
        return inCommittedTransaction(serverSession.getKeycloakSessionFactory(), parameter, what, onCommit);
    }

    protected static <T, R> R inCommittedTransaction(
            KeycloakSessionFactory sessionFactory,
            T parameter,
            BiFunction<KeycloakSession, T, R> what,
            BiConsumer<KeycloakSession, T> onCommit) {
        AtomicReference<R> result = new AtomicReference<>();
        runJobInTransaction(sessionFactory, session -> {
            if (session.getContext().getRealm() == null) {
                session.getContext().setRealm(session.realms().getRealmByName("master"));
            }
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    if (onCommit != null) {
                        onCommit.accept(session, parameter);
                    }
                }

                @Override
                protected void rollbackImpl() {
                    // Unsupported by these tests.
                }
            });
            result.set(what.apply(session, parameter));
        });
        return result.get();
    }

    protected static <R> R withRealm(
            KeycloakSession serverSession, String realmId, BiFunction<KeycloakSession, RealmModel, R> what) {
        return inCommittedTransaction(serverSession, session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            if (realm == null) {
                realm = session.realms().getRealmByName(realmId);
            }
            session.getContext().setRealm(realm);
            return what.apply(session, realm);
        });
    }

    protected static void withRealm(
            KeycloakSession serverSession, String realmId, BiConsumer<KeycloakSession, RealmModel> what) {
        withRealm(serverSession, realmId, (session, realm) -> {
            what.accept(session, realm);
            return null;
        });
    }

    protected static <R> R withCleanRealm(
            KeycloakSession serverSession, BiFunction<KeycloakSession, RealmModel, R> what) {
        return inCommittedTransaction(serverSession, session -> {
            String realmName = "clean-realm-" + UUID.randomUUID();
            RealmModel realm = session.realms().createRealm(realmName, realmName);
            session.getContext().setRealm(realm);

            try {
                return what.apply(session, realm);
            } finally {
                session.realms().removeRealm(realm.getId());
            }
        });
    }

    protected static void withCleanRealm(KeycloakSession serverSession, BiConsumer<KeycloakSession, RealmModel> what) {
        withCleanRealm(serverSession, (session, realm) -> {
            what.accept(session, realm);
            return null;
        });
    }

    protected static <T extends Provider> void withCleanRealmAndProvider(
            KeycloakSession serverSession, Function<KeycloakSession, T> providerFn, BiConsumer<T, RealmModel> what) {
        withCleanRealm(serverSession, (session, realm) -> {
            T provider = providerFn.apply(session);
            what.accept(provider, realm);
            return null;
        });
    }

    protected static <T extends Provider> void withRealmAndProvider(
            KeycloakSession serverSession,
            String realmId,
            Function<KeycloakSession, T> providerFn,
            BiConsumer<T, RealmModel> what) {
        withRealm(serverSession, realmId, (session, realm) -> {
            T provider = providerFn.apply(session);
            what.accept(provider, realm);
            return null;
        });
    }
}
