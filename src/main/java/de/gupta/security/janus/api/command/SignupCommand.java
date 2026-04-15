package de.gupta.security.janus.api.command;

import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;

public record SignupCommand(String loginIdentifier, String rawSecret, SignupProfileAttributes profileAttributes,
                            String requestedProvider)
{
	public static SignupCommand of(final String loginIdentifier, final String rawSecret,
	                               final SignupProfileAttributes profileAttributes, final String requestedProvider)
	{
		return new SignupCommand(loginIdentifier, rawSecret, profileAttributes, requestedProvider);
	}

	public SignupCommand
	{
		StringSanitizationUtility.requireNotBlank(loginIdentifier, "loginIdentifier must not be blank");
		StringSanitizationUtility.requireNotBlank(rawSecret, "rawSecret must not be blank");
		Objects.requireNonNull(profileAttributes, "profileAttributes must not be null");
		StringSanitizationUtility.requireNotBlank(requestedProvider, "requestedProvider must not be blank");
	}
}