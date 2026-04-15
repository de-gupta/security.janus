package de.gupta.security.janus.api;

import de.gupta.security.janus.adapter.local.LocalAccountIdentityView;
import de.gupta.security.janus.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.common.ProviderSession;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationFailure;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationFailureReason;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationSuccess;
import de.gupta.security.janus.domain.model.provider.ProviderSigninFailure;
import de.gupta.security.janus.domain.model.provider.ProviderSigninFailureReason;
import de.gupta.security.janus.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.domain.model.provider.ProviderSigninSuccess;
import de.gupta.security.janus.domain.model.provider.ProviderSignupFailure;
import de.gupta.security.janus.domain.model.provider.ProviderSignupFailureReason;
import de.gupta.security.janus.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.domain.model.provider.ProviderSignupSuccess;
import de.gupta.security.janus.domain.model.signin.SigninFailure;
import de.gupta.security.janus.domain.model.signin.SigninFailureReason;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.domain.model.signup.SignupFailure;
import de.gupta.security.janus.domain.model.signup.SignupFailureReason;
import de.gupta.security.janus.domain.model.signup.SignupResult;
import de.gupta.security.janus.domain.model.signup.SignupSuccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationServiceFactory")
final class AuthenticationServiceFactoryTest
{
	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-04-15T00:00:00Z"), ZoneOffset.UTC);

	@Test
	@DisplayName("signup succeeds when provider and local account creation both succeed")
	void signup_success() 
	{
		final ProviderIdentity providerIdentity = ProviderIdentity.of("external-123", "local", Map.of("tenant", "acme"));
		final ProviderSession providerSession = ProviderSession.of("local",
				Optional.of("session-1"),
				Optional.of("access-token"),
				Optional.empty(),
				Optional.empty(),
				Optional.of(FIXED_CLOCK.instant()),
				Optional.of(FIXED_CLOCK.instant().plusSeconds(900)),
				Map.of("scope", "basic"));
		final LocalAccountReference localAccount = LocalAccountReference.of("local-1",
				Optional.of("external-123"),
				"local",
				true);

		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupSuccess.of(providerIdentity, Optional.of(providerSession)),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(localAccount),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SignupResult result = service.signup(signupCommand());

		assertThat(result).isInstanceOf(SignupSuccess.class);
		final SignupSuccess success = (SignupSuccess) result;
		assertThat(success.providerIdentity()).isEqualTo(providerIdentity);
		assertThat(success.localAccount()).isEqualTo(localAccount);
		assertThat(success.providerSession()).contains(providerSession);
		assertThat(success.completion().providerAccountCreated()).isTrue();
		assertThat(success.completion().localAccountCreated()).isTrue();
		assertThat(success.completion().sessionEstablished()).isTrue();
	}

	@Test
	@DisplayName("signup fails cleanly on duplicate local identity")
	void signup_duplicateLocalIdentity()
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupSuccess.of(ProviderIdentity.of("external-123", "local"),
								Optional.empty()),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> true,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SignupResult result = service.signup(signupCommand());

		assertThat(result).isInstanceOf(SignupFailure.class);
		assertThat(((SignupFailure) result).reason()).isEqualTo(SignupFailureReason.DUPLICATE_LOCAL_ACCOUNT);
	}

	@Test
	@DisplayName("signup returns reconciliation needed when provider succeeds but local creation fails")
	void signup_reconciliationNeeded()
	{
		final ProviderIdentity providerIdentity = ProviderIdentity.of("external-123", "local");
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupSuccess.of(providerIdentity, Optional.empty()),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationFailure.of(LocalAccountCreationFailureReason.UNAVAILABLE,
								"database offline"),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SignupResult result = service.signup(signupCommand());

		assertThat(result).isInstanceOf(SignupFailure.class);
		final SignupFailure failure = (SignupFailure) result;
		assertThat(failure.reason()).isEqualTo(SignupFailureReason.RECONCILIATION_REQUIRED);
		assertThat(failure.providerIdentity()).contains(providerIdentity);
		assertThat(failure.details()).contains("database offline");
	}

	@Test
	@DisplayName("signin succeeds for a provider authenticated user linked to a local account")
	void signin_linkedLocalAccount()
	{
		final ProviderIdentity providerIdentity = ProviderIdentity.of("external-123", "local");
		final ProviderSession providerSession = ProviderSession.of("local",
				Optional.of("session-1"),
				Optional.of("access-token"),
				Optional.empty(),
				Optional.empty(),
				Optional.of(FIXED_CLOCK.instant()),
				Optional.of(FIXED_CLOCK.instant().plusSeconds(900)),
				Map.of());
		final LocalAccountIdentityView localAccount = new TestLocalAccount("local-1",
				Optional.of("external-123"),
				"local",
				true);

		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninSuccess.of(providerIdentity, providerSession)),
						(provider, externalSubject) -> Optional.of(localAccount),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(signinCommand());

		assertThat(result).isInstanceOf(SigninSuccess.class);
		final SigninSuccess success = (SigninSuccess) result;
		assertThat(success.providerIdentity()).isEqualTo(providerIdentity);
		assertThat(success.providerSession()).isEqualTo(providerSession);
		assertThat(success.localAccount()).contains(LocalAccountReference.of("local-1",
				Optional.of("external-123"),
				"local",
				true));
	}

	@Test
	@DisplayName("signin succeeds without a linked local account when policy allows it")
	void signin_unlinkedAllowed()
	{
		final ProviderIdentity providerIdentity = ProviderIdentity.of("external-123", "supabase");
		final ProviderSession providerSession = ProviderSession.of("supabase");
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninSuccess.of(providerIdentity, providerSession)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"supabase",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.allowingUnlinkedLocalSignin(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(signinCommand("supabase"));

		assertThat(result).isInstanceOf(SigninSuccess.class);
		assertThat(((SigninSuccess) result).localAccount()).isEmpty();
	}

	@Test
	@DisplayName("signin fails when linked local account is disabled")
	void signin_disabledLocalAccount()
	{
		final ProviderIdentity providerIdentity = ProviderIdentity.of("external-123", "local");
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninSuccess.of(providerIdentity, ProviderSession.of("local"))),
						(provider, externalSubject) -> Optional.of(new TestLocalAccount("local-1",
								Optional.of("external-123"),
								"local",
								false)),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(signinCommand());

		assertThat(result).isInstanceOf(SigninFailure.class);
		assertThat(((SigninFailure) result).reason()).isEqualTo(SigninFailureReason.LOCAL_ACCOUNT_DISABLED);
	}

	@Test
	@DisplayName("signin fails on invalid credentials")
	void signin_invalidCredentials()
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INVALID_CREDENTIALS, "wrong password")),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(signinCommand());

		assertThat(result).isInstanceOf(SigninFailure.class);
		final SigninFailure failure = (SigninFailure) result;
		assertThat(failure.reason()).isEqualTo(SigninFailureReason.INVALID_CREDENTIALS);
		assertThat(failure.details()).contains("wrong password");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("signupFailureMappings")
	@DisplayName("signup maps provider failures into typed Janus failures")
	void signup_failureMappings(final String description,
	                           final ProviderSignupFailure providerFailure,
	                           final SignupFailureReason expectedReason)
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(providerFailure, ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SignupResult result = service.signup(signupCommand());

		assertThat(result).isInstanceOf(SignupFailure.class);
		assertThat(((SignupFailure) result).reason()).isEqualTo(expectedReason);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("signinFailureMappings")
	@DisplayName("signin maps provider failures into typed Janus failures")
	void signin_failureMappings(final String description,
	                           final ProviderSigninFailure providerFailure,
	                           final SigninFailureReason expectedReason)
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR), providerFailure),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(signinCommand());

		assertThat(result).isInstanceOf(SigninFailure.class);
		assertThat(((SigninFailure) result).reason()).isEqualTo(expectedReason);
	}

	@Test
	@DisplayName("signup rejects a null command as invalid input")
	void signup_nullCommand()
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SignupResult result = service.signup(null);

		assertThat(result).isInstanceOf(SignupFailure.class);
		assertThat(((SignupFailure) result).reason()).isEqualTo(SignupFailureReason.INVALID_INPUT);
	}

	@Test
	@DisplayName("signin rejects a null command as malformed")
	void signin_nullCommand()
	{
		final AuthenticationService service = AuthenticationServiceFactory.create(
				AuthenticationConfiguration.of(
						identityProvider(ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
								ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR)),
						(provider, externalSubject) -> Optional.empty(),
						command -> LocalAccountCreationSuccess.of(LocalAccountReference.of("local-1",
								Optional.of("external-123"),
								"local",
								true)),
						loginIdentifier -> false,
						AuthenticationPolicy.defaults(),
						FIXED_CLOCK));

		final SigninResult result = service.signin(null);

		assertThat(result).isInstanceOf(SigninFailure.class);
		assertThat(((SigninFailure) result).reason()).isEqualTo(SigninFailureReason.MALFORMED_COMMAND);
	}

	private static Stream<Arguments> signupFailureMappings()
	{
		return Stream.of(
				Arguments.of("duplicate provider identity",
						ProviderSignupFailure.of(ProviderSignupFailureReason.DUPLICATE_IDENTITY),
						SignupFailureReason.DUPLICATE_PROVIDER_IDENTITY),
				Arguments.of("provider unavailable",
						ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_UNAVAILABLE),
						SignupFailureReason.PROVIDER_UNAVAILABLE),
				Arguments.of("provider rejected",
						ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_REJECTED),
						SignupFailureReason.PROVIDER_REJECTED),
				Arguments.of("unsupported provider",
						ProviderSignupFailure.of(ProviderSignupFailureReason.UNSUPPORTED_PROVIDER),
						SignupFailureReason.UNSUPPORTED_PROVIDER),
				Arguments.of("malformed command",
						ProviderSignupFailure.of(ProviderSignupFailureReason.MALFORMED_COMMAND),
						SignupFailureReason.INVALID_INPUT),
				Arguments.of("internal provider error",
						ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR),
						SignupFailureReason.UNEXPECTED_INTERNAL_FAILURE));
	}

	private static Stream<Arguments> signinFailureMappings()
	{
		return Stream.of(
				Arguments.of("provider unavailable",
						ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_UNAVAILABLE),
						SigninFailureReason.PROVIDER_UNAVAILABLE),
				Arguments.of("provider rejected",
						ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_REJECTED),
						SigninFailureReason.PROVIDER_REJECTED),
				Arguments.of("unsupported provider",
						ProviderSigninFailure.of(ProviderSigninFailureReason.UNSUPPORTED_PROVIDER),
						SigninFailureReason.UNSUPPORTED_PROVIDER),
				Arguments.of("malformed command",
						ProviderSigninFailure.of(ProviderSigninFailureReason.MALFORMED_COMMAND),
						SigninFailureReason.MALFORMED_COMMAND),
				Arguments.of("internal provider error",
						ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR),
						SigninFailureReason.UNEXPECTED_INTERNAL_FAILURE));
	}

	private static IdentityProviderPort identityProvider(final ProviderSignupResult signupResult,
	                                                     final ProviderSigninResult signinResult)
	{
		return new IdentityProviderPort()
		{
			@Override
			public ProviderSignupResult signup(final SignupProviderCommand command)
			{
				return signupResult;
			}

			@Override
			public ProviderSigninResult signin(final SigninProviderCommand command)
			{
				return signinResult;
			}
		};
	}

	private static SignupCommand signupCommand()
	{
		return SignupCommand.of("alice",
				"secret",
				SignupProfileAttributes.of(Optional.of("alice@example.com"),
						Optional.of("Alice"),
						Optional.of("Example"),
						Optional.of("Alice Example")),
				"local");
	}

	private static SigninCommand signinCommand()
	{
		return signinCommand("local");
	}

	private static SigninCommand signinCommand(final String provider)
	{
		return SigninCommand.of("alice", "secret", provider);
	}

	private record TestLocalAccount(String localAccountId,
	                                Optional<String> externalSubject,
	                                String provider,
	                                boolean active) implements LocalAccountIdentityView
	{
	}
}
