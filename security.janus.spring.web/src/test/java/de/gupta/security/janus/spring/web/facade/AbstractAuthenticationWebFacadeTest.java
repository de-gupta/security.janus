package de.gupta.security.janus.spring.web.facade;

import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.api.command.SignupProfileAttributes;
import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;
import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.core.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.core.domain.model.signup.SignupCompletion;
import de.gupta.security.janus.core.domain.model.signup.SignupResult;
import de.gupta.security.janus.core.domain.model.signup.SignupSuccess;
import de.gupta.security.janus.spring.web.adapter.SigninRequestToCommandAdapter;
import de.gupta.security.janus.spring.web.adapter.SigninResultToWebResponseAdapter;
import de.gupta.security.janus.spring.web.adapter.SignupRequestToCommandAdapter;
import de.gupta.security.janus.spring.web.adapter.SignupResultToWebResponseAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AbstractAuthenticationWebFacade")
class AbstractAuthenticationWebFacadeTest
{
	@Test
	@DisplayName("should depend directly on AuthenticationService instead of a Janus application service layer")
	void shouldDependDirectlyOnAuthenticationServiceInsteadOfAJanusApplicationServiceLayer()
	{
		final Constructor<?> constructor = TestFacade.class.getDeclaredConstructors()[0];

		assertThat(constructor.getParameterTypes()[0]).isEqualTo(AuthenticationService.class);
	}

	private static final class TestAuthenticationService implements AuthenticationService
	{
		private final SignupResult signupResult = Fixtures.signupResult();
		private final SigninResult signinResult = Fixtures.signinResult();
		private SignupCommand lastSignupCommand;
		private SigninCommand lastSigninCommand;

		@Override
		public SignupResult signup(final SignupCommand command)
		{
			lastSignupCommand = command;
			return signupResult;
		}

		@Override
		public SigninResult signin(final SigninCommand command)
		{
			lastSigninCommand = command;
			return signinResult;
		}
	}

	private static final class TestFacade
			extends AbstractAuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody>
	{
		private TestFacade(
				final AuthenticationService authenticationService,
				final SignupRequestToCommandAdapter<SignupRequest> signupRequestAdapter,
				final SigninRequestToCommandAdapter<SigninRequest> signinRequestAdapter,
				final SignupResultToWebResponseAdapter<SignupBody> signupResponseAdapter,
				final SigninResultToWebResponseAdapter<SigninBody> signinResponseAdapter)
		{
			super(authenticationService,
					signupRequestAdapter,
					signinRequestAdapter,
					signupResponseAdapter,
					signinResponseAdapter);
		}
	}

	private record SignupRequest(String username, String password)
	{
	}

	private record SigninRequest(String username, String password)
	{
	}

	private record SignupBody(String status, String subject)
	{
	}

	private record SigninBody(String status, String localAccountId)
	{
	}

	private static final class Fixtures
	{
		private static SignupCommand signupCommand()
		{
			return SignupCommand.of("ada@example.com",
					"correct horse battery staple",
					SignupProfileAttributes.of(Optional.of("ada@example.com"),
							Optional.of("Ada"),
							Optional.of("Lovelace"),
							Optional.of("Ada Lovelace")),
					"keycloak");
		}

		private static SigninCommand signinCommand()
		{
			return SigninCommand.of("ada@example.com", "correct horse battery staple", "keycloak");
		}

		private static SignupResult signupResult()
		{
			return SignupSuccess.of(ProviderIdentity.of("subject-1", "keycloak", Map.of("email", "ada@example.com")),
					LocalAccountReference.of("local-1", Optional.of("subject-1"), "keycloak", true),
					Optional.of(ProviderSession.of("keycloak",
							Optional.of("session-1"),
							Optional.of("access-token"),
							Optional.of("refresh-token"),
							Optional.empty(),
							Optional.of(Instant.parse("2026-04-18T09:00:00Z")),
							Optional.of(Instant.parse("2026-04-18T10:00:00Z")),
							Map.of("scope", "openid"))),
					SignupCompletion.completed(true));
		}

		private static SigninResult signinResult()
		{
			return SigninSuccess.of(ProviderSession.of("keycloak",
							Optional.of("session-1"),
							Optional.of("access-token"),
							Optional.of("refresh-token"),
							Optional.empty(),
							Optional.of(Instant.parse("2026-04-18T09:00:00Z")),
							Optional.of(Instant.parse("2026-04-18T10:00:00Z")),
							Map.of("scope", "openid")),
					ProviderIdentity.of("subject-1", "keycloak", Map.of("email", "ada@example.com")),
					Optional.of(LocalAccountReference.of("local-1", Optional.of("subject-1"), "keycloak", true)));
		}
	}

	@Nested
	@DisplayName("signup")
	class Signup
	{
		@Test
		@DisplayName("should map signup requests through Janus and adapt the result")
		void shouldMapSignupRequestsThroughJanusAndAdaptTheResult()
		{
			final TestAuthenticationService authenticationService = new TestAuthenticationService();
			final TestFacade facade = new TestFacade(authenticationService,
					_ -> Fixtures.signupCommand(),
					_ -> Fixtures.signinCommand(),
					_ -> AuthenticationWebResponse.of(HttpStatus.CREATED,
							new SignupBody("signup-ok", "subject-1")),
					_ -> AuthenticationWebResponse.of(HttpStatus.OK, new SigninBody("signin-ok", "local-1")));

			assertThat(facade.signup(new SignupRequest("ada@example.com", "correct horse battery staple")))
					.isEqualTo(
							AuthenticationWebResponse.of(HttpStatus.CREATED, new SignupBody("signup-ok", "subject-1")));
			assertThat(authenticationService.lastSignupCommand).isEqualTo(Fixtures.signupCommand());
		}
	}

	@Nested
	@DisplayName("signin")
	class Signin
	{
		@Test
		@DisplayName("should map signin requests through Janus and adapt the result")
		void shouldMapSigninRequestsThroughJanusAndAdaptTheResult()
		{
			final TestAuthenticationService authenticationService = new TestAuthenticationService();
			final TestFacade facade = new TestFacade(authenticationService,
					_ -> Fixtures.signupCommand(),
					_ -> Fixtures.signinCommand(),
					_ -> AuthenticationWebResponse.of(HttpStatus.CREATED,
							new SignupBody("signup-ok", "subject-1")),
					_ -> AuthenticationWebResponse.of(HttpStatus.OK, new SigninBody("signin-ok", "local-1")));

			assertThat(facade.signin(new SigninRequest("ada@example.com", "correct horse battery staple")))
					.isEqualTo(AuthenticationWebResponse.of(HttpStatus.OK, new SigninBody("signin-ok", "local-1")));
			assertThat(authenticationService.lastSigninCommand).isEqualTo(Fixtures.signinCommand());
		}
	}
}