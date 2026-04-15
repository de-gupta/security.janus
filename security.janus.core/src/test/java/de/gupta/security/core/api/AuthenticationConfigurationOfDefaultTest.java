package de.gupta.security.core.api;

import de.gupta.security.core.support.TestAuthenticationContext;
import de.gupta.security.janus.core.api.AuthenticationConfiguration;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthenticationConfiguration.of(identityProviderPort, localAccountLookupPort, localAccountCreationPort, clock)")
class AuthenticationConfigurationOfDefaultTest
{

	private record NullDependencyCase(String description,
	                                  Supplier<AuthenticationConfiguration> invocation,
	                                  String expectedMessage)
	{
	}

	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should apply default optional and policy values")
		void shouldApplyDefaultOptionalAndPolicyValues()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final AuthenticationConfiguration configuration =
					AuthenticationConfiguration.of(context.identityProviderPort,
							context.localAccountLookupPort,
							context.localAccountCreationPort,
							context.clock);

			assertThat(configuration.identityProviderPort()).as("identity provider should be retained")
			                                                .isSameAs(context.identityProviderPort);
			assertThat(configuration.localAccountLookupPort()).as("lookup port should be retained")
			                                                  .isSameAs(context.localAccountLookupPort);
			assertThat(configuration.localAccountCreationPort()).as("creation port should be retained")
			                                                    .isSameAs(context.localAccountCreationPort);
			assertThat(configuration.localAccountDuplicateCheckPort()).as("duplicate check should default to empty")
			                                                          .isEmpty();
			assertThat(configuration.authenticationPolicy()).as("policy should default to the library default")
			                                                .isEqualTo(AuthenticationPolicy.defaults());
			assertThat(configuration.clock()).as("clock should be retained").isSameAs(context.clock);
		}
	}

	@Nested
	@DisplayName("invalid dependencies")
	class InvalidDependencies
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("nullDependencyCases")
		@DisplayName("should reject null required dependencies")
		void shouldRejectNullRequiredDependencies(final String description,
		                                          final Supplier<AuthenticationConfiguration> invocation,
		                                          final String expectedMessage)
		{
			assertThatThrownBy(invocation::get)
					.as(description)
					.isInstanceOf(NullPointerException.class)
					.hasMessage(expectedMessage);
		}

		private static Stream<Arguments> nullDependencyCases()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			return Stream.of(
								 new NullDependencyCase("null identity provider should fail",
										 () -> AuthenticationConfiguration.of(null,
												 context.localAccountLookupPort,
												 context.localAccountCreationPort,
												 context.clock),
										 "identityProviderPort must not be null"),
								 new NullDependencyCase("null lookup port should fail",
										 () -> AuthenticationConfiguration.of(context.identityProviderPort,
												 null,
												 context.localAccountCreationPort,
												 context.clock),
										 "localAccountLookupPort must not be null"),
								 new NullDependencyCase("null creation port should fail",
										 () -> AuthenticationConfiguration.of(context.identityProviderPort,
												 context.localAccountLookupPort,
												 null,
												 context.clock),
										 "localAccountCreationPort must not be null"),
								 new NullDependencyCase("null clock should fail",
										 () -> AuthenticationConfiguration.of(context.identityProviderPort,
												 context.localAccountLookupPort,
												 context.localAccountCreationPort,
												 null),
										 "clock must not be null"))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedMessage()));
		}
	}
}