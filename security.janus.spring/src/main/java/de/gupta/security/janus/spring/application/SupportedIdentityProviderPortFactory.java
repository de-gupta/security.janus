package de.gupta.security.janus.spring.application;

import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.idp.implementation.BuiltInIdentityProviderConfiguration;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderConfiguration;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderPort;

import java.util.List;

final class SupportedIdentityProviderPortFactory
{
	private final SpringBeanLookup beanLookup;

	ResolutionResult<IdentityProviderPort> resolve()
	{
		final List<String> beanNames = beanLookup.beanNames(BuiltInIdentityProviderConfiguration.class);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.missing("No built-in identity provider configuration bean found.");
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous("Multiple beans found for 'BuiltInIdentityProviderConfiguration': " +
					beanNames +
					". Provide exactly one built-in identity provider configuration bean.");
		}

		final BuiltInIdentityProviderConfiguration configuration =
				beanLookup.bean(beanNames.getFirst(), BuiltInIdentityProviderConfiguration.class);

		return switch (configuration)
		{
			case KeycloakIdentityProviderConfiguration keycloakConfiguration ->
					ResolutionResult.resolved(KeycloakIdentityProviderPort.create(keycloakConfiguration));
		};
	}

	SupportedIdentityProviderPortFactory(final SpringBeanLookup beanLookup)
	{
		this.beanLookup = beanLookup;
	}
}
