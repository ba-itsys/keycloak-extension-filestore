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

import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.MigrationManager;

public class FileStoreMigrationManager implements MigrationManager {

    private static final Logger LOG = Logger.getLogger(FileStoreMigrationManager.class);

    @Override
    public void migrate() {
        LOG.info("Skipping migration manager as filestore migration is not supported");
    }

    @Override
    public void migrate(RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrate();
    }
}
