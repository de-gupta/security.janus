package de.gupta.security.janus.domain.model.provider;

import java.util.Objects;
import java.util.Optional;

public record ProviderSigninFailure(ProviderSigninFailureReason reason,
                                    Optional<String> details) implements ProviderSigninResult
{
	public static ProviderSigninFailure of(final ProviderSigninFailureReason reason)
	{
		return new ProviderSigninFailure(reason, Optional.empty());
	}

	public static ProviderSigninFailure of(final ProviderSigninFailureReason reason, final String details)
	{
		return new ProviderSigninFailure(reason, Optional.of(Objects.requireNonNull(details, "details must not be null")));
	}

	public ProviderSigninFailure
	{
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(details, "details must not be null");
	}
}
