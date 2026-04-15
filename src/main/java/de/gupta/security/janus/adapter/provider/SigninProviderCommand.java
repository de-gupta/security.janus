package de.gupta.security.janus.adapter.provider;

public record SigninProviderCommand(String loginIdentifier, String rawSecret, String requestedProvider)
{
	public static SigninProviderCommand of(final String loginIdentifier,
	                                       final String rawSecret,
	                                       final String requestedProvider)
	{
		return new SigninProviderCommand(loginIdentifier, rawSecret, requestedProvider);
	}
}