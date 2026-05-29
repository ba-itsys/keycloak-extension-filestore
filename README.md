[![CI](https://github.com/opdt/keycloak-extension-filestore/workflows/CI/badge.svg)](https://github.com/opdt/keycloak-extension-filestore/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/de.arbeitsagentur.opdt/keycloak-extension-filestore.svg)](https://search.maven.org/artifact/de.arbeitsagentur.opdt/keycloak-extension-filestore)

# keycloak-extension-filestore

Implements client, clientscope, group, realm, role file-based storage.
Initially forked from Keycloak's own experimental version.

Intended to be used in read-only filesystems, for example mounted K8s-configmaps (but can also be used to interactively create the configuration by using the admin console)
To use this, you most likely have to implement your own `DatastoreProvider` and mix it with a different implementation to store users, sessions etc.

# Caveats

This extension currently does NOT support Keycloak Organizations and thus must be run with the corresponding feature flag turned off.

## Maven Coordinates

```xml
<dependency>
    <groupId>de.arbeitsagentur.opdt</groupId>
    <artifactId>keycloak-extension-filestore</artifactId>
    <version>1.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Keycloak Default Filestore Baseline

`DefaultRealmBaselineTest` compares the filestore that Keycloak creates on startup with the checked-in
baseline in `src/test/resources/baselines/keycloak-default-filestore`.

The baseline is intentionally a real filestore snapshot from the last reviewed Keycloak version. When the
project Keycloak version is updated, this test should show whether Keycloak changed default realm files,
default authentication flow entries, policy attributes, or other generated defaults.

Run the check with:

```shell
mvn -Dtest=DefaultRealmBaselineTest test
```

If it fails during a Keycloak version update, inspect the reported file and the git diff. The comparison
normalizes generated ids, generated key material, and YAML indentation, so reported differences should be
semantic changes in the default filestore. A `keycloak-version.txt` mismatch is expected when comparing a
new Keycloak version against an older accepted baseline; content changes in files such as `master.yaml`
show what changed.

While an older baseline is intentionally kept for review, a full `mvn test` run is expected to fail on this
test. After the changed defaults are accepted, regenerate the baseline so the full suite is green again.

After reviewing and accepting the changes, update the checked-in baseline with:

```shell
mvn -Dtest=DefaultRealmBaselineTest -Dkeycloak.default-realm-baseline.update=true test
```

Update mode writes the new baseline and then runs the same comparison against the files it wrote. After
that succeeds, run the full suite:

```shell
mvn test
```

Then review and commit the resulting diff under
`src/test/resources/baselines/keycloak-default-filestore`.

To compare a future Keycloak version against an older baseline, keep the baseline from the last reviewed
Keycloak version, then update the project version and run the test normally. The baseline source version is
recorded in `keycloak-version.txt`.
