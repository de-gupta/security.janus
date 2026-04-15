package de.gupta.security.janus.api;

import de.gupta.security.janus.support.TestAuthenticationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthenticationConfiguration.of(identityProviderPort, localAccountLookupPort, localAccountCreationPort, localAccountDuplicateCheckPort, authenticationPolicy, clock)")
class AuthenticationConfigurationOfCustomTest
{
	private static Stream<Arguments> nullDependencyCases()
	{
		final TestAuthenticationContext context = new TestAuthenticationContext();

		return Stream.of(
							 new NullDependencyCase("null identity provider should fail",
									 () -> AuthenticationConfiguration.of(null,
											 context.localAccountLookupPort,
											 context.localAccountCreationPort,
											 context.localAccountDuplicateCheckPort,
											 AuthenticationPolicy.defaults(),
											 context.clock),
									 Optional.of("identityProviderPort must not be null")),
							 new NullDependencyCase("null lookup port should fail",
									 () -> AuthenticationConfiguration.of(context.identityProviderPort,
											 null,
											 context.localAccountCreationPort,
											 context.localAccountDuplicateCheckPort,
											 AuthenticationPolicy.defaults(),
											 context.clock),
									 Optional.of("localAccountLookupPort must not be null")),
							 new NullDependencyCase("null creation port should fail",
									 () -> AuthenticationConfiguration.of(context.identityProviderPort,
											 context.localAccountLookupPort,
											 null,
											 context.localAccountDuplicateCheckPort,
											 AuthenticationPolicy.defaults(),
											 context.clock),
									 Optional.of("localAccountCreationPort must not be null")),
							 new NullDependencyCase("null duplicate check should fail",
									 () -> AuthenticationConfiguration.of(context.identityProviderPort,
											 context.localAccountLookupPort,
											 context.localAccountCreationPort,
											 null,
											 AuthenticationPolicy.defaults(),
											 context.clock),
									 Optional.empty()),
							 new NullDependencyCase("null policy should fail",
									 () -> AuthenticationConfiguration.of(context.identityProviderPort,
											 context.localAccountLookupPort,
											 context.localAccountCreationPort,
											 context.localAccountDuplicateCheckPort,
											 null,
											 context.clock),
									 Optional.of("authenticationPolicy must not be null")),
							 new NullDependencyCase("null clock should fail",
									 () -> AuthenticationConfiguration.of(context.identityProviderPort,
											 context.localAccountLookupPort,
											 context.localAccountCreationPort,
											 context.localAccountDuplicateCheckPort,
											 AuthenticationPolicy.defaults(),
											 null),
									 Optional.of("clock must not be null")))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
							 testCase.expectedMessage()));
	}

	private record NullDependencyCase(String description,
	                                  Supplier<AuthenticationConfiguration> invocation,
	                                  Optional<String> expectedMessage)
	{
	}

	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should retain all supplied collaborators")
		void shouldRetainAllSuppliedCollaborators()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final AuthenticationConfiguration configuration =
					AuthenticationConfiguration.of(context.identityProviderPort,
							context.localAccountLookupPort,
							context.localAccountCreationPort,
							context.localAccountDuplicateCheckPort,
							AuthenticationPolicy.allowingUnlinkedLocalSignin(),
							context.clock);

			assertThat(configuration.localAccountDuplicateCheckPort()).as("duplicate check port should be wrapped")
			                                                          .containsSame(
																			  context.localAccountDuplicateCheckPort);
			assertThat(configuration.authenticationPolicy()).as("policy should be retained").isEqualTo(
					AuthenticationPolicy.allowingUnlinkedLocalSignin());
		}
	}

	@Nested
	@DisplayName("invalid dependencies")
	class InvalidDependencies
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.AuthenticationConfigurationOfCustomTest#nullDependencyCases")
		@DisplayName("should reject null required dependencies")
		void shouldRejectNullRequiredDependencies(final String description,
		                                          final Supplier<AuthenticationConfiguration> invocation,
		                                          final Optional<String> expectedMessage)
		{
			assertThatThrownBy(invocation::get)
					.as(description)
					.isInstanceOf(NullPointerException.class)
					.satisfies(throwable -> expectedMessage.ifPresent(message ->
							assertThat(throwable).as("exception message").hasMessage(message)));
		}
	}
}