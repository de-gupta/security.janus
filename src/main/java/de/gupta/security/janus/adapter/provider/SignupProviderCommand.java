package de.gupta.security.janus.adapter.provider;

import de.gupta.commons.utility.string.StringSanitizationUtility;
import de.gupta.security.janus.api.command.SignupProfileAttributes;

import java.util.Objects;

public record SignupProviderCommand(String loginIdentifier,
                                    String rawSecret,
                                    SignupProfileAttributes profileAttributes,
                                    String requestedProvider)
{
	public static SignupProviderCommand of(final String loginIdentifier,
	                                       final String rawSecret,
	                                       final SignupProfileAttributes profileAttributes,
	                                       final String requestedProvider)
	{
		return new SignupProviderCommand(loginIdentifier, rawSecret, profileAttributes, requestedProvider);
	}

	public SignupProviderCommand
	{
		StringSanitizationUtility.requireNotBlank(loginIdentifier, "loginIdentifier must not be blank");
		StringSanitizationUtility.requireNotBlank(rawSecret, "rawSecret must not be blank");
		Objects.requireNonNull(profileAttributes, "profileAttributes must not be null");
		StringSanitizationUtility.requireNotBlank(requestedProvider, "requestedProvider must not be blank");
	}
}
