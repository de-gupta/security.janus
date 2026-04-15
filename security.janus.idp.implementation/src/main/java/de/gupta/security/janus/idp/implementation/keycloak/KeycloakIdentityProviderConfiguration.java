package de.gupta.security.janus.idp.implementation.keycloak;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record KeycloakIdentityProviderConfiguration(String serverUrl,
                                                    String realm,
                                                    String providerAlias,
                                                    String userClientId,
                                                    Optional<String> userClientSecret,
                                                    String adminClientId,
                                                    Optional<String> adminClientSecret,
                                                    String adminRealm,
                                                    boolean signInAfterSignup,
                                                    Optional<String> scope,
                                                    Duration connectTimeout,
                                                    Clock clock)
{
	public static KeycloakIdentityProviderConfiguration of(final String serverUrl,
	                                                       final String realm,
	                                                       final String userClientId,
	                                                       final String adminClientId)
	{
		return new KeycloakIdentityProviderConfiguration(serverUrl,
				realm,
				"keycloak",
				userClientId,
				Optional.empty(),
				adminClientId,
				Optional.empty(),
				realm,
				true,
				Optional.of("openid"),
				Duration.ofSeconds(10),
				Clock.systemUTC());
	}

	public static KeycloakIdentityProviderConfiguration of(final String serverUrl,
	                                                       final String realm,
	                                                       final String providerAlias,
	                                                       final String userClientId,
	                                                       final Optional<String> userClientSecret,
	                                                       final String adminClientId,
	                                                       final Optional<String> adminClientSecret,
	                                                       final String adminRealm,
	                                                       final boolean signInAfterSignup,
	                                                       final Optional<String> scope,
	                                                       final Duration connectTimeout,
	                                                       final Clock clock)
	{
		return new KeycloakIdentityProviderConfiguration(serverUrl,
				realm,
				providerAlias,
				userClientId,
				userClientSecret,
				adminClientId,
				adminClientSecret,
				adminRealm,
				signInAfterSignup,
				scope,
				connectTimeout,
				clock);
	}

	public KeycloakIdentityProviderConfiguration
	{
		requireNotBlank(serverUrl, "serverUrl must not be blank");
		requireNotBlank(realm, "realm must not be blank");
		requireNotBlank(providerAlias, "providerAlias must not be blank");
		requireNotBlank(userClientId, "userClientId must not be blank");
		requireNotBlank(adminClientId, "adminClientId must not be blank");
		requireNotBlank(adminRealm, "adminRealm must not be blank");
		requireOptional(userClientSecret, "userClientSecret must not be null");
		requireOptional(adminClientSecret, "adminClientSecret must not be null");
		requireOptional(scope, "scope must not be null");
		Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
	}

	private static void requireNotBlank(final String value, final String message)
	{
		if (value == null || value.isBlank())
		{
			throw new IllegalArgumentException(message);
		}
	}

	private static void requireOptional(final Optional<String> value, final String message)
	{
		Objects.requireNonNull(value, message);
	}
}
