package de.gupta.security.janus.domain.model.provider;

import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.common.ProviderSession;
import java.util.Objects;

public record ProviderSigninSuccess(ProviderIdentity providerIdentity,
                                    ProviderSession providerSession) implements ProviderSigninResult
{
	public static ProviderSigninSuccess of(final ProviderIdentity providerIdentity,
	                                       final ProviderSession providerSession)
	{
		return new ProviderSigninSuccess(providerIdentity, providerSession);
	}

	public ProviderSigninSuccess
	{
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
		Objects.requireNonNull(providerSession, "providerSession must not be null");
	}
}
