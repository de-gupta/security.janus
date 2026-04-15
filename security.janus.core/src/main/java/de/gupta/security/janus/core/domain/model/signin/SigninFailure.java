package de.gupta.security.janus.core.domain.model.signin;

import java.util.Objects;
import java.util.Optional;

public record SigninFailure(SigninFailureReason reason, Optional<String> details) implements SigninResult
{
	public static SigninFailure of(final SigninFailureReason reason)
	{
		return new SigninFailure(reason, Optional.empty());
	}

	public static SigninFailure of(final SigninFailureReason reason, final String details)
	{
		return new SigninFailure(reason, Optional.of(Objects.requireNonNull(details, "details must not be null")));
	}

	public SigninFailure
	{
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(details, "details must not be null");
	}
}