# GDCC Keycloak Dataverse Extension

A [Keycloak Service Provider Interface (SPI)](https://www.keycloak.org/docs/latest/server_development/) that lets
[Keycloak](https://www.keycloak.org/) authenticate **Dataverse built-in users** directly against the Dataverse database
they already live in.

In other words: *Dataverse keeps its users, Keycloak becomes the login page.*

## Why

Dataverse ships its own account system ("built-in users"), with usernames and password hashes stored in its database.
Turning Keycloak into the identity provider for a Dataverse installation would normally mean migrating or
duplicating those accounts - and with them, every password hash.

This extension avoids that. It plugs into Keycloak as a *User Storage Provider*, so it looks up and verifies credentials
where they already are, and Dataverse authenticates against Keycloak via a standard OIDC provider configuration.
No credential copying, no second user directory to keep in sync.

## What it does

- **Authenticates existing Dataverse built-in users**
  through Keycloak's normal login flow, using the Dataverse password hashes as-is.
- **Understands Dataverse's password schemes.**
  Credential checking mirrors the hashing logic used by Dataverse itself, so users keep working without password reset.
  (Supported algorithms: SHA and BCrypt, selected per user by the stored encryption version)
- **Looks users up by id, username, or email**,
  case-insensitively, and federates them into the Keycloak realm on demand.
  Name and email are served from the Dataverse `AuthenticatedUser` database record.
- **Passes identity through to tokens.**
  Federated users are tagged with the provider id, so downstream mappers and clients know where an assertion came from.
- **Configures its datasource per realm component**, 
  so a realm decides which database it reads (see the `datasource` config property).

## What it deliberately does *not* do

- **It is read-only.**
  Users are not created, updated, locked, or deleted through Keycloak.
  Dataverse remains the system of record for account data.
- **It only validates passwords.**
  Other credential types (OTP, WebAuthn, ...) are not supported.
- **It does not replace other Keycloak providers.**
  It is one federation source among many and is intended to sit alongside standard identity providers.

## Components

The repository contains a single deployable artifact:

| Component                       | Provider id                      | Type                  |
|---------------------------------|----------------------------------|-----------------------|
| Dataverse built-in user storage | `dv-builtin-users-authenticator` | `UserStorageProvider` |

Alongside it, you will find supporting material for running and exercising the extension against a real stack:
a sample realm definition, an example Dataverse IDC authentication provider configuration, and container definitions
that bring up a Keycloak instance with the extension installed next to a Dataverse instance sharing the same database.

> The bundled container setup is a development and testing harness, not a deployment
> blueprint. It uses fixed credentials and dev-mode settings and should stay well away from
> anything production-shaped.

## Requirements

- Keycloak 26.x (the extension is tracked against the current Keycloak release line)
- Java 21, matching the Keycloak server baseline
- A PostgreSQL database holding a Dataverse schema
- The JDBC driver for that database available to Keycloak

## Status

Early, ongoing development work.

The API surface, build layout, and the accompanying development setup are all still settling,
so expect changes and no stability guarantees yet.

## License

[Apache License, Version 2.0](./LICENSE)
