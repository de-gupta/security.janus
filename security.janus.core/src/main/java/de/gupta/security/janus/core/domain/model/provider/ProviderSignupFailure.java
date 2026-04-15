package de.gupta.security.janus.core.domain.model.provider;

import java.util.Objects;
import java.util.Optional;

public record ProviderSignupFailure(ProviderSignupFailureReason reason,
                                    Optional<String> details) implements ProviderSignupResult
{
	public static ProviderSignupFailure of(final ProviderSignupFailureReason reason)
	{
		return new ProviderSignupFailure(reason, Optional.empty());
	}

	public static ProviderSignupFailure of(final ProviderSignupFailureReason reason, final String details)
	{
		return new ProviderSignupFailure(reason, Optional.of(Objects.requireNonNull(details, "details must not be null")));
	}

	public ProviderSignupFailure
	{
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(details, "details must not be null");
	}
}