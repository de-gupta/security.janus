package de.gupta.security.janus.api.command;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;
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

	public SignupProfileAttributes
	{
		email = sanitize(email, "email must not be null");
		firstName = sanitize(firstName, "firstName must not be null");
		lastName = sanitize(lastName, "lastName must not be null");
		displayName = sanitize(displayName, "displayName must not be null");
	}

	private static Optional<String> sanitize(final Optional<String> value, final String message)
	{
		return Unfolding.augur(value)
				.metamorphose(String::trim)
				.unlace(v -> StringSanitizationUtility.requireNotBlank(v, message))
				.optional();
	}
}