package de.gupta.security.janus.domain.model.provider;

import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.common.ProviderSession;
import java.util.Objects;
import java.util.Optional;

public record ProviderSignupSuccess(ProviderIdentity providerIdentity,
                                    Optional<ProviderSession> providerSession) implements ProviderSignupResult
{
	public static ProviderSignupSuccess of(final ProviderIdentity providerIdentity,
	                                       final Optional<ProviderSession> providerSession)
	{
		return new ProviderSignupSuccess(providerIdentity, providerSession);
	}

	public ProviderSignupSuccess
	{
		Objects.requireNonNull(providerIdentity, "providerIdentity must not be null");
		Objects.requireNonNull(providerSession, "providerSession must not be null");
	}
}
