package de.gupta.security.janus.idp.implementation.keycloak;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KeycloakIdentityProviderConfiguration.of")
class KeycloakIdentityProviderConfigurationOfTest
{
	private record InvalidArgumentCase(String description,
	                                   Supplier<KeycloakIdentityProviderConfiguration> invocation,
	                                   Class<? extends Throwable> expectedType,
	                                   String expectedMessage)
	{
	}

	@Nested
	@DisplayName("factory defaults")
	class FactoryDefaults
	{
		@Test
		@DisplayName("should create the default keycloak configuration")
		void shouldCreateTheDefaultKeycloakConfiguration()
		{
			assertThat(KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
					"janus",
					"janus-app",
					"janus-admin"))
					.extracting(KeycloakIdentityProviderConfiguration::serverUrl,
							KeycloakIdentityProviderConfiguration::realm,
							KeycloakIdentityProviderConfiguration::providerAlias,
							KeycloakIdentityProviderConfiguration::userClientId,
							KeycloakIdentityProviderConfiguration::userClientSecret,
							KeycloakIdentityProviderConfiguration::adminClientId,
							KeycloakIdentityProviderConfiguration::adminClientSecret,
							KeycloakIdentityProviderConfiguration::adminRealm,
							KeycloakIdentityProviderConfiguration::signInAfterSignup,
							KeycloakIdentityProviderConfiguration::scope,
							KeycloakIdentityProviderConfiguration::connectTimeout,
							KeycloakIdentityProviderConfiguration::clock)
					.containsExactly("http://localhost:8080",
							"janus",
							"keycloak",
							"janus-app",
							Optional.empty(),
							"janus-admin",
							Optional.empty(),
							"janus",
							true,
							Optional.of("openid"),
							Duration.ofSeconds(10),
							Clock.systemUTC());
		}

		@Test
		@DisplayName("should create a custom configuration")
		void shouldCreateACustomConfiguration()
		{
			final Clock clock = Clock.fixed(java.time.Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC);

			assertThat(KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
					"janus",
					"sso",
					"janus-app",
					Optional.of("user-secret"),
					"janus-admin",
					Optional.of("admin-secret"),
					"master",
					false,
					Optional.of("openid profile email"),
					Duration.ofSeconds(2),
					clock))
					.extracting(KeycloakIdentityProviderConfiguration::providerAlias,
							KeycloakIdentityProviderConfiguration::userClientSecret,
							KeycloakIdentityProviderConfiguration::adminClientSecret,
							KeycloakIdentityProviderConfiguration::adminRealm,
							KeycloakIdentityProviderConfiguration::signInAfterSignup,
							KeycloakIdentityProviderConfiguration::scope,
							KeycloakIdentityProviderConfiguration::connectTimeout,
							KeycloakIdentityProviderConfiguration::clock)
					.containsExactly("sso",
							Optional.of("user-secret"),
							Optional.of("admin-secret"),
							"master",
							false,
							Optional.of("openid profile email"),
							Duration.ofSeconds(2),
							clock);
		}
	}

	@Nested
	@DisplayName("invalid arguments")
	class InvalidArguments
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("invalidArgumentCases")
		@DisplayName("should reject invalid configuration arguments")
		void shouldRejectInvalidConfigurationArguments(final String description,
		                                               final Supplier<KeycloakIdentityProviderConfiguration> invocation,
		                                               final Class<? extends Throwable> expectedType,
		                                               final String expectedMessage)
		{
			assertThatThrownBy(invocation::get)
					.as(description)
					.isInstanceOf(expectedType)
					.hasMessage(expectedMessage);
		}

		private static Stream<Arguments> invalidArgumentCases()
		{
			final Clock clock = Clock.systemUTC();
			final Duration connectTimeout = Duration.ofSeconds(2);

			return Stream.of(
								 new InvalidArgumentCase("blank server url",
										 () -> KeycloakIdentityProviderConfiguration.of(" ",
												 "janus",
												 "keycloak",
												 "janus-app",
												 Optional.empty(),
												 "janus-admin",
												 Optional.empty(),
												 "master",
												 true,
												 Optional.of("openid"),
												 connectTimeout,
												 clock),
										 IllegalArgumentException.class,
										 "serverUrl must not be blank"),
								 new InvalidArgumentCase("blank provider alias",
										 () -> KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
												 "janus",
												 " ",
												 "janus-app",
												 Optional.empty(),
												 "janus-admin",
												 Optional.empty(),
												 "master",
												 true,
												 Optional.of("openid"),
												 connectTimeout,
												 clock),
										 IllegalArgumentException.class,
										 "providerAlias must not be blank"),
								 new InvalidArgumentCase("null user client secret optional",
										 () -> KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
												 "janus",
												 "keycloak",
												 "janus-app",
												 null,
												 "janus-admin",
												 Optional.empty(),
												 "master",
												 true,
												 Optional.of("openid"),
												 connectTimeout,
												 clock),
										 NullPointerException.class,
										 "userClientSecret must not be null"),
								 new InvalidArgumentCase("null connect timeout",
										 () -> KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
												 "janus",
												 "keycloak",
												 "janus-app",
												 Optional.empty(),
												 "janus-admin",
												 Optional.empty(),
												 "master",
												 true,
												 Optional.of("openid"),
												 null,
												 clock),
										 NullPointerException.class,
										 "connectTimeout must not be null"),
								 new InvalidArgumentCase("null clock",
										 () -> KeycloakIdentityProviderConfiguration.of("http://localhost:8080",
												 "janus",
												 "keycloak",
												 "janus-app",
												 Optional.empty(),
												 "janus-admin",
												 Optional.empty(),
												 "master",
												 true,
												 Optional.of("openid"),
												 connectTimeout,
												 null),
										 NullPointerException.class,
										 "clock must not be null"))
			             .map(testCase -> Arguments.of(testCase.description(),
								 testCase.invocation(),
								 testCase.expectedType(),
								 testCase.expectedMessage()));
		}
	}
}