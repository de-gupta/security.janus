package de.gupta.security.janus.core.adapter.local;

import de.gupta.security.janus.core.api.command.SignupProfileAttributes;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;

import java.util.Optional;

public record LocalAccountCreationCommand(String loginIdentifier,
                                          SignupProfileAttributes profileAttributes,
                                          String requestedProvider,
                                          ProviderIdentity providerIdentity,
                                          Optional<ProviderSession> providerSession)
{
	public static LocalAccountCreationCommand of(final String loginIdentifier,
	                                             final SignupProfileAttributes profileAttributes,
	                                             final String requestedProvider,
	                                             final ProviderIdentity providerIdentity,
	                                             final Optional<ProviderSession> providerSession)
	{
		return new LocalAccountCreationCommand(loginIdentifier,
				profileAttributes,
				requestedProvider,
				providerIdentity,
				providerSession);
	}
}