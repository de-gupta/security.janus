package de.gupta.security.janus.core.domain.model.signin;

import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;
import java.util.Objects;
import java.util.Optional;

public record SigninSuccess(ProviderSession providerSession,
                            ProviderIdentity providerIdentity,
                            Optional<LocalAccountReference> localAccount) implements SigninResult
{
	public static SigninSuccess of(final ProviderSession providerSession,
	                               final ProviderIdentity providerIdentity,
	                               final Optional<LocalAccountReference> localAccount)
	{
		return new SigninSuccess(providerSession, providerIdentity, localAccount);
	}

	public SigninSuccess
	{
		Objects.requireNonNull(providerSession, "providerSession must not be null");
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
		Objects.requireNonNull(localAccount, "localAccount must not be null");
	}
}