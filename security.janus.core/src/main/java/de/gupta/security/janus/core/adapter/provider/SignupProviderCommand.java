package de.gupta.security.janus.core.adapter.provider;

import de.gupta.security.janus.core.api.command.SignupProfileAttributes;

public record SignupProviderCommand(String loginIdentifier, String rawSecret, SignupProfileAttributes profileAttributes,
                                    String requestedProvider)
{
	public static SignupProviderCommand of(final String loginIdentifier, final String rawSecret,
	                                       final SignupProfileAttributes profileAttributes,
	                                       final String requestedProvider)
	{
		return new SignupProviderCommand(loginIdentifier, rawSecret, profileAttributes, requestedProvider);
	}
}