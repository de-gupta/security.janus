package de.gupta.security.janus.domain.model.local;

import java.util.Objects;
import java.util.Optional;

public record LocalAccountCreationFailure(LocalAccountCreationFailureReason reason,
                                          Optional<String> details) implements LocalAccountCreationResult
{
	public static LocalAccountCreationFailure of(final LocalAccountCreationFailureReason reason)
	{
		return new LocalAccountCreationFailure(reason, Optional.empty());
	}

	public static LocalAccountCreationFailure of(final LocalAccountCreationFailureReason reason, final String details)
	{
		return new LocalAccountCreationFailure(reason,
				Optional.of(Objects.requireNonNull(details, "details must not be null")));
	}

	public LocalAccountCreationFailure
	{
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(details, "details must not be null");
	}
}
