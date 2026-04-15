package de.gupta.security.janus.core.domain.model.signup;

import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;
import java.util.Objects;
import java.util.Optional;

public record SignupSuccess(ProviderIdentity providerIdentity,
                            LocalAccountReference localAccount,
                            Optional<ProviderSession> providerSession,
                            SignupCompletion completion) implements SignupResult
{
	public static SignupSuccess of(final ProviderIdentity providerIdentity,
	                               final LocalAccountReference localAccount,
	                               final Optional<ProviderSession> providerSession,
	                               final SignupCompletion completion)
	{
		return new SignupSuccess(providerIdentity, localAccount, providerSession, completion);
	}

	public SignupSuccess
	{
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
		Objects.requireNonNull(localAccount, "localAccount must not be null");
		Objects.requireNonNull(providerSession, "providerSession must not be null");
		Objects.requireNonNull(completion, "completion must not be null");
	}
}