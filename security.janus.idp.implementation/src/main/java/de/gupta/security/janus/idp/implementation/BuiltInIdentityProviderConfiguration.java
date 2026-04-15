package de.gupta.security.janus.idp.implementation;

import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderConfiguration;

public sealed interface BuiltInIdentityProviderConfiguration permits KeycloakIdentityProviderConfiguration
{
}
