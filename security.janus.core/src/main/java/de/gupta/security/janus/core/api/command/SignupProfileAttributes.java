package de.gupta.security.janus.core.api.command;

import java.util.Optional;

public record SignupProfileAttributes(Optional<String> email, Optional<String> firstName, Optional<String> lastName,
                                      Optional<String> displayName)
{
	public static SignupProfileAttributes empty()
	{
		return of(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}

	public static SignupProfileAttributes of(final Optional<String> email, final Optional<String> firstName,
	                                         final Optional<String> lastName, final Optional<String> displayName)
	{
		return new SignupProfileAttributes(email, firstName, lastName, displayName);
	}
}