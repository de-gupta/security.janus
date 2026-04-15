package de.gupta.security.janus.api;

import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.domain.model.provider.ProviderSigninFailure;
import de.gupta.security.janus.domain.model.provider.ProviderSigninFailureReason;
import de.gupta.security.janus.domain.model.signin.SigninFailure;
import de.gupta.security.janus.domain.model.signin.SigninFailureReason;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.support.TestAuthenticationContext;
import de.gupta.security.janus.support.TestFixtures;
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

@DisplayName("AuthenticationService.signin")
class AuthenticationServiceSigninTest
{
	private static Stream<Arguments> invalidCommandCases()
	{
		return Stream.of(
							 new InvalidCommandCase("null command", () -> null),
							 new InvalidCommandCase("null login identifier", () -> new SigninCommand(null, "secret", "local")),
							 new InvalidCommandCase("blank login identifier", () -> new SigninCommand(" ", "secret", "local")),
							 new InvalidCommandCase("null raw secret", () -> new SigninCommand("ada@example.com", null, "local")),
							 new InvalidCommandCase("blank raw secret", () -> new SigninCommand("ada@example.com", " ", "local")),
							 new InvalidCommandCase("null requested provider",
									 () -> new SigninCommand("ada@example.com", "secret", null)),
							 new InvalidCommandCase("blank requested provider",
									 () -> new SigninCommand("ada@example.com", "secret", " ")))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.commandSupplier()));
	}

	private static Stream<Arguments> providerFailureCases()
	{
		return Stream.of(
							 new ProviderFailureCase("invalid credentials",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.INVALID_CREDENTIALS, "bad password"),
									 SigninFailureReason.INVALID_CREDENTIALS, Optional.of("bad password")),
							 new ProviderFailureCase("provider unavailable",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_UNAVAILABLE),
									 SigninFailureReason.PROVIDER_UNAVAILABLE, Optional.empty()),
							 new ProviderFailureCase("provider rejected request",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_REJECTED, "captcha required"),
									 SigninFailureReason.PROVIDER_REJECTED, Optional.of("captcha required")),
							 new ProviderFailureCase("unsupported provider",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.UNSUPPORTED_PROVIDER),
									 SigninFailureReason.UNSUPPORTED_PROVIDER, Optional.empty()),
							 new ProviderFailureCase("malformed provider command",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.MALFORMED_COMMAND, "provider input bad"),
									 SigninFailureReason.MALFORMED_COMMAND, Optional.of("provider input bad")),
							 new ProviderFailureCase("internal provider error",
									 ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR),
									 SigninFailureReason.UNEXPECTED_INTERNAL_FAILURE, Optional.empty()))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.providerFailure(),
							 testCase.expectedReason(), testCase.expectedDetails()));
	}

	private record InvalidCommandCase(String description, Supplier<SigninCommand> commandSupplier)
	{
	}

	private record ProviderFailureCase(String description,
	                                   ProviderSigninFailure providerFailure,
	                                   SigninFailureReason expectedReason,
	                                   Optional<String> expectedDetails)
	{
	}

	@Nested
	@DisplayName("validation")
	class Validation
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.AuthenticationServiceSigninTest#invalidCommandCases")
		@DisplayName("should return malformed command for invalid signin payloads")
		void shouldReturnMalformedCommandForInvalidSigninPayloads(final String description,
		                                                          final Supplier<SigninCommand> commandSupplier)
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final SigninResult result = context.serviceWithoutDuplicateCheck().signin(commandSupplier.get());

			assertThat(result)
					.as(description)
					.isInstanceOf(SigninFailure.class)
					.extracting(SigninFailure.class::cast)
					.extracting(SigninFailure::reason, SigninFailure::details)
					.containsExactly(SigninFailureReason.MALFORMED_COMMAND, Optional.of("signin command is malformed"));
			assertThat(context.identityProviderPort.receivedSigninCommand).as(
					"provider should not be called for malformed commands").isNull();
		}
	}

	@Nested
	@DisplayName("provider failures")
	class ProviderFailures
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.AuthenticationServiceSigninTest#providerFailureCases")
		@DisplayName("should map provider failures to public signin failures")
		void shouldMapProviderFailuresToPublicSigninFailures(final String description,
		                                                     final ProviderSigninFailure providerFailure,
		                                                     final SigninFailureReason expectedReason,
		                                                     final Optional<String> expectedDetails)
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.identityProviderPort.signinHandler = ignored -> providerFailure;

			final SigninResult result = context.service().signin(TestFixtures.signinCommand());

			assertThat(result)
					.as(description)
					.isInstanceOf(SigninFailure.class)
					.extracting(SigninFailure.class::cast)
					.satisfies(failure ->
					{
						assertThat(failure.reason()).as("mapped reason").isEqualTo(expectedReason);
						assertThat(failure.details()).as("mapped details").isEqualTo(expectedDetails);
					});
			assertThat(context.localAccountLookupPort.receivedProvider).as(
					"local lookup should not run when provider signin fails").isNull();
		}
	}

	@Nested
	@DisplayName("local linkage")
	class LocalLinkage
	{
		@Test
		@DisplayName("should fail when no linked local account exists and policy disallows unlinked signin")
		void shouldFailWhenNoLinkedLocalAccountExistsAndPolicyDisallowsUnlinkedSignin()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.localAccountLookupPort.lookupHandler = ignored -> Optional.empty();

			final SigninResult result = context.service().signin(TestFixtures.signinCommand());

			assertThat(result)
					.as("missing local account should fail by default")
					.isInstanceOf(SigninFailure.class)
					.extracting(SigninFailure.class::cast)
					.extracting(SigninFailure::reason, SigninFailure::details)
					.containsExactly(SigninFailureReason.LOCAL_ACCOUNT_NOT_LINKED,
							Optional.of("no linked local account found"));
		}

		@Test
		@DisplayName("should allow unlinked signin when policy explicitly permits it")
		void shouldAllowUnlinkedSigninWhenPolicyExplicitlyPermitsIt()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.localAccountLookupPort.lookupHandler = ignored -> Optional.empty();

			final SigninResult result = context.service(AuthenticationPolicy.allowingUnlinkedLocalSignin())
			                                   .signin(TestFixtures.signinCommand());

			assertThat(result)
					.as("policy should allow provider-only success")
					.isInstanceOf(SigninSuccess.class)
					.extracting(SigninSuccess.class::cast)
					.satisfies(success ->
					{
						assertThat(success.providerIdentity()).as("provider identity")
						                                      .isEqualTo(TestFixtures.providerIdentity());
						assertThat(success.providerSession()).as("provider session")
						                                     .isEqualTo(TestFixtures.providerSession());
						assertThat(success.localAccount()).as("local account").isEmpty();
					});
		}
	}

	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should fail when linked local account is disabled")
		void shouldFailWhenLinkedLocalAccountIsDisabled()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.localAccountLookupPort.lookupHandler = key -> Optional.of(
					new TestAuthenticationContext.LocalAccountIdentityViewStub("local-account-1",
							Optional.of(key.externalSubject()), key.provider(), false));

			final SigninResult result = context.service().signin(TestFixtures.signinCommand());

			assertThat(result)
					.as("disabled account should block signin")
					.isInstanceOf(SigninFailure.class)
					.extracting(SigninFailure.class::cast)
					.extracting(SigninFailure::reason, SigninFailure::details)
					.containsExactly(SigninFailureReason.LOCAL_ACCOUNT_DISABLED,
							Optional.of("linked local account is disabled"));
		}

		@Test
		@DisplayName("should return provider session and linked local account")
		void shouldReturnProviderSessionAndLinkedLocalAccount()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final SigninResult result = context.service().signin(TestFixtures.signinCommand());

			assertThat(result)
					.as("successful signin should return SigninSuccess")
					.isInstanceOf(SigninSuccess.class)
					.extracting(SigninSuccess.class::cast)
					.satisfies(success ->
					{
						assertThat(success.providerIdentity()).as("provider identity")
						                                      .isEqualTo(TestFixtures.providerIdentity());
						assertThat(success.providerSession()).as("provider session")
						                                     .isEqualTo(TestFixtures.providerSession());
						assertThat(success.localAccount()).as("linked local account")
						                                  .contains(TestFixtures.localAccountReference());
					});
		}
	}
}