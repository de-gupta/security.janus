package de.gupta.security.janus.api;

import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.domain.model.provider.ProviderSignupFailure;
import de.gupta.security.janus.domain.model.provider.ProviderSignupFailureReason;
import de.gupta.security.janus.domain.model.signup.SignupFailure;
import de.gupta.security.janus.domain.model.signup.SignupFailureReason;
import de.gupta.security.janus.domain.model.signup.SignupResult;
import de.gupta.security.janus.domain.model.signup.SignupSuccess;
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

@DisplayName("AuthenticationService.signup")
class AuthenticationServiceSignupTest
{
	private static Stream<Arguments> invalidCommandCases()
	{
		return Stream.of(
							 new InvalidCommandCase("null command", () -> null),
							 new InvalidCommandCase("null login identifier",
									 () -> new SignupCommand(null, "secret", SignupProfileAttributes.empty(), "local")),
							 new InvalidCommandCase("blank login identifier",
									 () -> new SignupCommand(" ", "secret", SignupProfileAttributes.empty(), "local")),
							 new InvalidCommandCase("null raw secret",
									 () -> new SignupCommand("ada@example.com", null, SignupProfileAttributes.empty(), "local")),
							 new InvalidCommandCase("blank raw secret",
									 () -> new SignupCommand("ada@example.com", " ", SignupProfileAttributes.empty(), "local")),
							 new InvalidCommandCase("null requested provider",
									 () -> new SignupCommand("ada@example.com", "secret", SignupProfileAttributes.empty(), null)),
							 new InvalidCommandCase("blank requested provider",
									 () -> new SignupCommand("ada@example.com", "secret", SignupProfileAttributes.empty(), " ")),
							 new InvalidCommandCase("null profile attributes",
									 () -> new SignupCommand("ada@example.com", "secret", null, "local")),
							 new InvalidCommandCase("null email optional", () -> new SignupCommand("ada@example.com", "secret",
									 new SignupProfileAttributes(null, Optional.empty(), Optional.empty(), Optional.empty()),
									 "local")),
							 new InvalidCommandCase("blank first name", () -> new SignupCommand("ada@example.com", "secret",
									 new SignupProfileAttributes(Optional.empty(), Optional.of(" "), Optional.empty(),
											 Optional.empty()), "local")),
							 new InvalidCommandCase("blank last name", () -> new SignupCommand("ada@example.com", "secret",
									 new SignupProfileAttributes(Optional.empty(), Optional.empty(), Optional.of(" "),
											 Optional.empty()), "local")),
							 new InvalidCommandCase("blank display name", () -> new SignupCommand("ada@example.com", "secret",
									 new SignupProfileAttributes(Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.of(" ")), "local")))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.commandSupplier()));
	}

	private static Stream<Arguments> providerFailureCases()
	{
		return Stream.of(
							 new ProviderFailureCase("duplicate provider identity",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.DUPLICATE_IDENTITY, "identity exists"),
									 SignupFailureReason.DUPLICATE_PROVIDER_IDENTITY, Optional.of("identity exists")),
							 new ProviderFailureCase("provider rejected request",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_REJECTED, "policy denied"),
									 SignupFailureReason.PROVIDER_REJECTED, Optional.of("policy denied")),
							 new ProviderFailureCase("provider unavailable",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_UNAVAILABLE),
									 SignupFailureReason.PROVIDER_UNAVAILABLE, Optional.empty()),
							 new ProviderFailureCase("unsupported provider",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.UNSUPPORTED_PROVIDER, "unknown provider"),
									 SignupFailureReason.UNSUPPORTED_PROVIDER, Optional.of("unknown provider")),
							 new ProviderFailureCase("malformed provider command",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.MALFORMED_COMMAND),
									 SignupFailureReason.INVALID_INPUT, Optional.empty()),
							 new ProviderFailureCase("internal provider error",
									 ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR, "boom"),
									 SignupFailureReason.UNEXPECTED_INTERNAL_FAILURE, Optional.of("boom")))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.providerFailure(),
							 testCase.expectedReason(), testCase.expectedDetails()));
	}

	private record InvalidCommandCase(String description, Supplier<SignupCommand> commandSupplier)
	{
	}

	private record ProviderFailureCase(String description,
	                                   ProviderSignupFailure providerFailure,
	                                   SignupFailureReason expectedReason,
	                                   Optional<String> expectedDetails)
	{
	}

	@Nested
	@DisplayName("validation")
	class Validation
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.AuthenticationServiceSignupTest#invalidCommandCases")
		@DisplayName("should return invalid input for malformed commands")
		void shouldReturnInvalidInputForMalformedCommands(final String description,
		                                                  final Supplier<SignupCommand> commandSupplier)
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final SignupResult result = context.serviceWithoutDuplicateCheck().signup(commandSupplier.get());

			assertThat(result)
					.as(description)
					.isInstanceOf(SignupFailure.class)
					.extracting(SignupFailure.class::cast)
					.extracting(SignupFailure::reason, SignupFailure::details)
					.containsExactly(SignupFailureReason.INVALID_INPUT, Optional.of("signup command is malformed"));
			assertThat(context.identityProviderPort.receivedSignupCommand).as(
					"provider should not be called for malformed commands").isNull();
			assertThat(context.localAccountCreationPort.receivedCommand).as(
					"local account creation should not run for malformed commands").isNull();
		}
	}

	@Nested
	@DisplayName("duplicate detection")
	class DuplicateDetection
	{
		@Test
		@DisplayName("should fail fast when duplicate check reports an existing local account")
		void shouldFailFastWhenDuplicateCheckReportsAnExistingLocalAccount()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.localAccountDuplicateCheckPort.duplicateHandler = ignored -> true;

			final SignupResult result = context.service().signup(TestFixtures.signupCommand());

			assertThat(result)
					.as("duplicate login identifier should be rejected")
					.isInstanceOf(SignupFailure.class)
					.extracting(SignupFailure.class::cast)
					.extracting(SignupFailure::reason, SignupFailure::details)
					.containsExactly(SignupFailureReason.DUPLICATE_LOCAL_ACCOUNT,
							Optional.of("local account already exists for login identifier"));
			assertThat(context.identityProviderPort.receivedSignupCommand).as(
					"provider should not be called when duplicate check fails").isNull();
		}

		@Test
		@DisplayName("should skip duplicate detection when no duplicate check port is configured")
		void shouldSkipDuplicateDetectionWhenNoDuplicateCheckPortIsConfigured()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			context.serviceWithoutDuplicateCheck().signup(TestFixtures.signupCommand());

			assertThat(context.localAccountDuplicateCheckPort.receivedLoginIdentifier).as(
					"no duplicate check port means no duplicate query").isNull();
			assertThat(context.identityProviderPort.receivedSignupCommand).as(
					"provider should still receive the signup request").isNotNull();
		}
	}

	@Nested
	@DisplayName("provider failures")
	class ProviderFailures
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.AuthenticationServiceSignupTest#providerFailureCases")
		@DisplayName("should map provider failures to public signup failures")
		void shouldMapProviderFailuresToPublicSignupFailures(final String description,
		                                                     final ProviderSignupFailure providerFailure,
		                                                     final SignupFailureReason expectedReason,
		                                                     final Optional<String> expectedDetails)
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.identityProviderPort.signupHandler = ignored -> providerFailure;

			final SignupResult result = context.service().signup(TestFixtures.signupCommand());

			assertThat(result)
					.as(description)
					.isInstanceOf(SignupFailure.class)
					.extracting(SignupFailure.class::cast)
					.satisfies(failure ->
					{
						assertThat(failure.reason()).as("mapped reason").isEqualTo(expectedReason);
						assertThat(failure.details()).as("mapped details").isEqualTo(expectedDetails);
						assertThat(failure.providerIdentity()).as("provider identity should be absent").isEmpty();
					});
			assertThat(context.localAccountCreationPort.receivedCommand).as(
					"local creation should not run after provider failure").isNull();
		}
	}

	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should request reconciliation when local account creation fails after provider success")
		void shouldRequestReconciliationWhenLocalAccountCreationFailsAfterProviderSuccess()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();
			context.localAccountCreationPort.createHandler = ignored ->
					de.gupta.security.janus.domain.model.local.LocalAccountCreationFailure.of(
							de.gupta.security.janus.domain.model.local.LocalAccountCreationFailureReason.INTERNAL_ERROR,
							"database unavailable");

			final SignupResult result = context.service().signup(TestFixtures.signupCommand());

			assertThat(result)
					.as("provider-first signup should surface reconciliation-required")
					.isInstanceOf(SignupFailure.class)
					.extracting(SignupFailure.class::cast)
					.satisfies(failure ->
					{
						assertThat(failure.reason()).as("failure reason")
						                            .isEqualTo(SignupFailureReason.RECONCILIATION_REQUIRED);
						assertThat(failure.details()).as("failure details").contains("database unavailable");
						assertThat(failure.providerIdentity()).as("provider identity should be preserved")
						                                      .contains(TestFixtures.providerIdentity());
					});
		}

		@Test
		@DisplayName("should create provider and local account and surface a rich success result")
		void shouldCreateProviderAndLocalAccountAndSurfaceARichSuccessResult()
		{
			final TestAuthenticationContext context = new TestAuthenticationContext();

			final SignupResult result = context.service().signup(TestFixtures.signupCommand());

			assertThat(result)
					.as("successful signup should return SignupSuccess")
					.isInstanceOf(SignupSuccess.class)
					.extracting(SignupSuccess.class::cast)
					.satisfies(success ->
					{
						assertThat(success.providerIdentity()).as("provider identity")
						                                      .isEqualTo(TestFixtures.providerIdentity());
						assertThat(success.localAccount()).as("local account")
						                                  .isEqualTo(TestFixtures.localAccountReference());
						assertThat(success.providerSession()).as("provider session")
						                                     .contains(TestFixtures.providerSession());
						assertThat(success.completion().providerAccountCreated()).as("provider account created")
						                                                         .isTrue();
						assertThat(success.completion().localAccountCreated()).as("local account created").isTrue();
						assertThat(success.completion().sessionEstablished()).as("session established").isTrue();
					});
		}
	}
}