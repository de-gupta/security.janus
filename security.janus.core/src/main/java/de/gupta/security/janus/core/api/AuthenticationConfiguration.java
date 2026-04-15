package de.gupta.security.janus.core.api;

import de.gupta.security.janus.core.adapter.local.LocalAccountCreationPort;
import de.gupta.security.janus.core.adapter.local.LocalAccountDuplicateCheckPort;
import de.gupta.security.janus.core.adapter.local.LocalAccountLookupPort;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public record AuthenticationConfiguration(IdentityProviderPort identityProviderPort,
                                          LocalAccountLookupPort localAccountLookupPort,
                                          LocalAccountCreationPort localAccountCreationPort,
                                          Optional<LocalAccountDuplicateCheckPort> localAccountDuplicateCheckPort,
                                          AuthenticationPolicy authenticationPolicy, Clock clock)
{
	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort)
	{
		return of(identityProviderPort, localAccountLookupPort, localAccountCreationPort, Clock.systemUTC());
	}

	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort,
	                                             final Clock clock)
	{
		return new AuthenticationConfiguration(identityProviderPort, localAccountLookupPort, localAccountCreationPort,
				Optional.empty(), AuthenticationPolicy.defaults(), clock);
	}

	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort,
	                                             final AuthenticationPolicy authenticationPolicy)
	{
		return of(identityProviderPort,
				localAccountLookupPort,
				localAccountCreationPort,
				authenticationPolicy,
				Clock.systemUTC());
	}

	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort,
	                                             final AuthenticationPolicy authenticationPolicy,
	                                             final Clock clock)
	{
		return new AuthenticationConfiguration(identityProviderPort, localAccountLookupPort, localAccountCreationPort,
				Optional.empty(), authenticationPolicy, clock);
	}

	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort,
	                                             final LocalAccountDuplicateCheckPort localAccountDuplicateCheckPort,
	                                             final AuthenticationPolicy authenticationPolicy)
	{
		return of(identityProviderPort,
				localAccountLookupPort,
				localAccountCreationPort,
				localAccountDuplicateCheckPort,
				authenticationPolicy,
				Clock.systemUTC());
	}

	public static AuthenticationConfiguration of(final IdentityProviderPort identityProviderPort,
	                                             final LocalAccountLookupPort localAccountLookupPort,
	                                             final LocalAccountCreationPort localAccountCreationPort,
	                                             final LocalAccountDuplicateCheckPort localAccountDuplicateCheckPort,
	                                             final AuthenticationPolicy authenticationPolicy, final Clock clock)
	{
		return new AuthenticationConfiguration(identityProviderPort, localAccountLookupPort, localAccountCreationPort,
				Optional.of(localAccountDuplicateCheckPort), authenticationPolicy, clock);
	}

	public AuthenticationConfiguration
	{
		Objects.requireNonNull(identityProviderPort, "identityProviderPort must not be null");
		Objects.requireNonNull(localAccountLookupPort, "localAccountLookupPort must not be null");
		Objects.requireNonNull(localAccountCreationPort, "localAccountCreationPort must not be null");
		Objects.requireNonNull(localAccountDuplicateCheckPort, "localAccountDuplicateCheckPort must not be null");
		Objects.requireNonNull(authenticationPolicy, "authenticationPolicy must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
	}
}
