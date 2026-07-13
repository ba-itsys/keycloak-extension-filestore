[![CI](https://github.com/opdt/keycloak-extension-filestore/workflows/CI/badge.svg)](https://github.com/opdt/keycloak-extension-filestore/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/de.arbeitsagentur.opdt/keycloak-extension-filestore.svg)](https://search.maven.org/artifact/de.arbeitsagentur.opdt/keycloak-extension-filestore)

# keycloak-extension-filestore

Implements client, clientscope, group, realm, role and identity-provider file-based storage.
Initially forked from Keycloak's own experimental version.

Intended to be used in read-only filesystems, for example mounted K8s-configmaps (but can also be used to interactively create the configuration by using the admin console).

The extension provides its own `DatastoreProvider` that serves the **config areas** (realms, clients, client scopes, roles, groups, identity providers) from files. Everything it does not serve — users, sessions, and other dynamic state — falls through to Keycloak's default storage, so it is normally combined with a store for those areas (see [Pairing with a dynamic store](#pairing-with-a-dynamic-store)).

Requires **Keycloak 26.7.0** or newer.

## How to use

- Put the JAR in Keycloak's `providers` folder.
- Select the file datastore with `--spi-datastore--provider=file` and enable the required preview feature with `--features=stateless`.
- Point the extension at the filestore directory with `--spi-map-storage--file--dir=<path>` (for example a mounted, read-only ConfigMap).

Once `file` is the selected datastore, the extension automatically disables the SPI providers that would otherwise shadow the file-backed config stores (the JPA realm provider and the realm / authorization / organization caches), so you no longer need to set those disables by hand. Features that are not supported still have to be turned off manually (see [Caveats](#caveats)).

| CLI-Parameter | Description |
|---------------|-------------|
| --spi-datastore--provider=file | Required — select the file datastore |
| --features=stateless | Required — keeps only an embedded local cache; moves dynamic state to the datastore |
| --spi-map-storage--file--dir=<path> | Directory the config files are read from / written to |
| --spi-datastore--file--resources-version-seed=<seed> | Optional — pins the theme `/resources/{tag}/` cache-buster to a stable value across replicas (recommended for rolling updates without a relational database) |
| --features-disabled=authorization,admin-fine-grained-authz,organization | Disable unsupported features |

### Pairing with a store for users and sessions

Filestore only serves the config areas; everything it does not serve falls through to Keycloak's default storage, so users and sessions come from whatever store is configured for them.

**Default: relational database (JPA).** No extra extension is needed — the usual JPA-backed user and session storage keeps working alongside filestore. Only the JPA *realm* provider is turned off (filestore owns realms); the JPA connection and the JPA user/session providers are untouched. This is the simplest setup: files for config, your database for users and sessions.

**Fully database-free: the Cassandra extension.** To drop the relational database entirely, pair with the [Cassandra extension](https://github.com/opdt/keycloak-cassandra-extension), which follows the same activation patterns. `file` stays the selected datastore and serves its config areas, while Cassandra serves the areas listed for it:

```
--spi-datastore--provider=file
--spi-datastore--cassandra--areas=user,user-session,auth-session,login-failure,single-use-object,revoked-token
```

With no relational database in that setup, this extension's deployment-state provider owns the theme resources tag (derive it deterministically with `--spi-datastore--file--resources-version-seed`).

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

## Local Development

### Prerequisites

- **JDK**: See `pom.xml` for the required Java version.
- **Maven**: Used for building the project.

### Checks (formatting and tests)

Run the following commands locally to ensure code quality:

- **Formatting**: `mvn spotless:apply` (Ensures consistent code style).
- **Verification**: `mvn verify` (Runs the full test suite and builds the project).

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

The test only fails when the generated default content actually changes (a changed file, or an added or
removed file). A Keycloak version bump on its own does **not** fail it: `keycloak-version.txt` is treated as
metadata that records which version last produced the baseline, and its value is not compared. So a routine
version update where Keycloak did not change any defaults keeps both this test and a full `mvn test` green
with no regeneration needed.

If it does fail during a Keycloak version update, inspect the reported file and the git diff. The comparison
normalizes generated ids, generated key material, and YAML indentation, so reported differences are
semantic changes in the default filestore; content changes in files such as `master.yaml` show what changed.

Regenerate the baseline only after such a change has been reviewed and accepted. Update the checked-in
baseline with:

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
