package de.gupta.security.janus.core.api.command;

public record SigninCommand(String loginIdentifier, String rawSecret, String requestedProvider)
{
	public static SigninCommand of(final String loginIdentifier, final String rawSecret, final String requestedProvider)
	{
		return new SigninCommand(loginIdentifier, rawSecret, requestedProvider);
	}
}