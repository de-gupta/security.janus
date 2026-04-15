package de.gupta.security.janus.api.command;

import de.gupta.commons.utility.string.StringSanitizationUtility;

public record SigninCommand(String loginIdentifier, String rawSecret, String requestedProvider)
{
	public static SigninCommand of(final String loginIdentifier, final String rawSecret, final String requestedProvider)
	{
		return new SigninCommand(loginIdentifier, rawSecret, requestedProvider);
	}

	public SigninCommand
	{
		StringSanitizationUtility.requireNotBlank(loginIdentifier, "loginIdentifier must not be blank");
		StringSanitizationUtility.requireNotBlank(rawSecret, "rawSecret must not be blank");
		StringSanitizationUtility.requireNotBlank(requestedProvider, "requestedProvider must not be blank");
	}
}