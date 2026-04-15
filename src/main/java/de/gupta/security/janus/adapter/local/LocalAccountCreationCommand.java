package de.gupta.security.janus.adapter.local;

import de.gupta.commons.utility.string.StringSanitizationUtility;
import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.common.ProviderSession;

import java.util.Objects;
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

	public LocalAccountCreationCommand
	{
		StringSanitizationUtility.requireNotBlank(loginIdentifier, "loginIdentifier must not be blank");
		Objects.requireNonNull(profileAttributes, "profileAttributes must not be null");
		StringSanitizationUtility.requireNotBlank(requestedProvider, "requestedProvider must not be blank");
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
		Objects.requireNonNull(providerSession, "providerSession must not be null");
	}
}
