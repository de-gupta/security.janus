package de.gupta.security.janus.domain.model.signup;

import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import java.util.Objects;
import java.util.Optional;

public record SignupFailure(SignupFailureReason reason,
                            Optional<String> details,
                            Optional<ProviderIdentity> providerIdentity) implements SignupResult
{
	public static SignupFailure of(final SignupFailureReason reason)
	{
		return new SignupFailure(reason, Optional.empty(), Optional.empty());
	}

	public static SignupFailure of(final SignupFailureReason reason, final String details)
	{
		return new SignupFailure(reason,
				Optional.of(Objects.requireNonNull(details, "details must not be null")),
				Optional.empty());
	}

	public static SignupFailure of(final SignupFailureReason reason,
	                               final String details,
	                               final ProviderIdentity providerIdentity)
	{
		return new SignupFailure(reason,
				Optional.of(Objects.requireNonNull(details, "details must not be null")),
				Optional.of(Objects.requireNonNull(providerIdentity, "providerIdentity must not be null")));
	}

	public SignupFailure
	{
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(details, "details must not be null");
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
	}
}
