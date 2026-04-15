package de.gupta.security.janus.core.api.command;

public record SignupCommand(String loginIdentifier, String rawSecret, SignupProfileAttributes profileAttributes,
                            String requestedProvider)
{
	public static SignupCommand of(final String loginIdentifier, final String rawSecret,
	                               final SignupProfileAttributes profileAttributes, final String requestedProvider)
	{
		return new SignupCommand(loginIdentifier, rawSecret, profileAttributes, requestedProvider);
	}
}