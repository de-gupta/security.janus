package de.gupta.security.janus.spring.application;

import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderConfiguration;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderPort;
import de.gupta.security.janus.spring.configuration.JanusSupportedIdentityProvider;

import java.util.List;

final class SupportedIdentityProviderPortFactory
{
	private final SpringBeanLookup beanLookup;

	ResolutionResult<IdentityProviderPort> resolve()
	{
		final List<String> beanNames = beanLookup.beanNames(JanusSupportedIdentityProvider.class);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.missing("No supported identity provider selection bean found.");
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous("Multiple beans found for 'JanusSupportedIdentityProvider': " +
					beanNames +
					". Provide exactly one supported identity provider selection bean.");
		}

		final JanusSupportedIdentityProvider provider =
				beanLookup.bean(beanNames.getFirst(), JanusSupportedIdentityProvider.class);

		return switch (provider)
		{
			case KEYCLOAK -> resolveKeycloak();
		};
	}

	private ResolutionResult<IdentityProviderPort> resolveKeycloak()
	{
		final List<String> beanNames = beanLookup.beanNames(KeycloakIdentityProviderConfiguration.class);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.missing("Unable to resolve configured identity provider 'KEYCLOAK'. " +
					"Provide exactly one KeycloakIdentityProviderConfiguration bean.");
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous("Multiple beans found for 'KeycloakIdentityProviderConfiguration': " +
					beanNames +
					". Provide exactly one KeycloakIdentityProviderConfiguration bean for KEYCLOAK.");
		}

		return ResolutionResult.resolved(KeycloakIdentityProviderPort.create(
				beanLookup.bean(beanNames.getFirst(), KeycloakIdentityProviderConfiguration.class)));
	}

	SupportedIdentityProviderPortFactory(final SpringBeanLookup beanLookup)
	{
		this.beanLookup = beanLookup;
	}
}
