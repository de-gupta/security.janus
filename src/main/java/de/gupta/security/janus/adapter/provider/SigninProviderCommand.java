package de.gupta.security.janus.adapter.provider;

import de.gupta.commons.utility.string.StringSanitizationUtility;

public record SigninProviderCommand(String loginIdentifier, String rawSecret, String requestedProvider)
{
	public static SigninProviderCommand of(final String loginIdentifier,
	                                       final String rawSecret,
	                                       final String requestedProvider)
	{
		return new SigninProviderCommand(loginIdentifier, rawSecret, requestedProvider);
	}

	public SigninProviderCommand
	{
		StringSanitizationUtility.requireNotBlank(loginIdentifier, "loginIdentifier must not be blank");
		StringSanitizationUtility.requireNotBlank(rawSecret, "rawSecret must not be blank");
		StringSanitizationUtility.requireNotBlank(requestedProvider, "requestedProvider must not be blank");
	}
}
