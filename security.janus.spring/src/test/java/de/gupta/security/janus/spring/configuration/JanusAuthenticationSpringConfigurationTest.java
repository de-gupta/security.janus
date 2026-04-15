package de.gupta.security.janus.spring.configuration;

import de.gupta.security.janus.core.adapter.local.LocalAccountCreationCommand;
import de.gupta.security.janus.core.adapter.local.LocalAccountIdentityView;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.core.domain.model.signin.SigninFailure;
import de.gupta.security.janus.core.domain.model.signin.SigninFailureReason;
import de.gupta.security.janus.core.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.core.domain.model.signup.SignupSuccess;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderConfiguration;
import de.gupta.security.janus.spring.support.SpringKeycloakStubServer;
import de.gupta.security.janus.spring.support.SpringTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JanusAuthenticationSpringConfiguration")
class JanusAuthenticationSpringConfigurationTest
{
	@Test
	@DisplayName("should create AuthenticationService from explicit Janus ports")
	void shouldCreateAuthenticationServiceFromExplicitJanusPorts()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, ExplicitPortsConfiguration.class);
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
			final SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort =
					context.getBean(SpringTestFixtures.RecordingIdentityProviderPort.class);
			final SpringTestFixtures.RecordingLocalAccountCreationPort localAccountCreationPort =
					context.getBean(SpringTestFixtures.RecordingLocalAccountCreationPort.class);

			assertThat(authenticationService.signup(SpringTestFixtures.signupCommand()))
					.isInstanceOf(SignupSuccess.class)
					.extracting(SignupSuccess.class::cast)
					.satisfies(signupSuccess ->
					{
						assertThat(signupSuccess.providerIdentity().metadata()).containsEntry("source",
								"explicit-provider");
						assertThat(signupSuccess.localAccount().localAccountId()).isEqualTo(
								"explicit-creation-local-account");
					});
			assertThat(identityProviderPort.signupCalls).isEqualTo(1);
			assertThat(localAccountCreationPort.calls).isEqualTo(1);
		}
	}

	@Test
	@DisplayName("should create AuthenticationService from fallback function beans")
	void shouldCreateAuthenticationServiceFromFallbackFunctionBeans()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, FunctionBeansConfiguration.class);
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
			final SpringTestFixtures.RecordingFunctionHandlers handlers =
					context.getBean(SpringTestFixtures.RecordingFunctionHandlers.class);

			assertThat(authenticationService.signup(SpringTestFixtures.signupCommand()))
					.isInstanceOf(SignupSuccess.class)
					.extracting(SignupSuccess.class::cast)
					.satisfies(signupSuccess ->
					{
						assertThat(signupSuccess.providerIdentity().metadata()).containsEntry("source",
								"function-provider");
						assertThat(signupSuccess.localAccount().localAccountId()).isEqualTo(
								"function-creation-local-account");
					});
			assertThat(authenticationService.signin(SpringTestFixtures.signinCommand()))
					.isInstanceOf(SigninSuccess.class)
					.extracting(SigninSuccess.class::cast)
					.satisfies(signinSuccess -> assertThat(signinSuccess.localAccount())
							.get()
							.extracting(LocalAccountReference::localAccountId)
							.isEqualTo("function-lookup-local-account"));
			assertThat(handlers.providerSignupCalls).isEqualTo(1);
			assertThat(handlers.providerSigninCalls).isEqualTo(1);
			assertThat(handlers.creationCalls).isEqualTo(1);
			assertThat(handlers.lookupCalls).isEqualTo(1);
		}
	}

	@Test
	@DisplayName("should create AuthenticationService from supported identity provider configuration")
	void shouldCreateAuthenticationServiceFromSupportedIdentityProviderConfiguration() throws Exception
	{
		try (SpringKeycloakStubServer keycloak = new SpringKeycloakStubServer();
		     AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			keycloak.stubAdminToken("admin-token");
			keycloak.stubCreateUser("keycloak-user-1");
			keycloak.stubResetPassword("keycloak-user-1");
			keycloak.stubPasswordGrantSuccess("keycloak-user-1", "ada@example.com");

			context.register(JanusAuthenticationSpringConfiguration.class);
			context.registerBean(JanusSupportedIdentityProvider.class, () -> JanusSupportedIdentityProvider.KEYCLOAK);
			context.registerBean(KeycloakIdentityProviderConfiguration.class,
					() -> keycloak.configuration(Clock.fixed(Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC)));
			context.registerBean(SpringTestFixtures.RecordingLocalAccountLookupPort.class,
					SpringTestFixtures.RecordingLocalAccountLookupPort::new);
			context.registerBean(SpringTestFixtures.RecordingLocalAccountCreationPort.class,
					SpringTestFixtures.RecordingLocalAccountCreationPort::new);
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);

			assertThat(authenticationService.signup(SpringTestFixtures.signupCommand("keycloak")))
					.isInstanceOf(SignupSuccess.class)
					.extracting(SignupSuccess.class::cast)
					.satisfies(signupSuccess ->
					{
						assertThat(signupSuccess.providerIdentity().provider()).isEqualTo("keycloak");
						assertThat(signupSuccess.providerIdentity().externalSubject()).isEqualTo("keycloak-user-1");
						assertThat(signupSuccess.providerIdentity().metadata()).containsEntry("realm", "janus");
					});
			assertThat(authenticationService.signin(SpringTestFixtures.signinCommand("keycloak")))
					.isInstanceOf(SigninSuccess.class);
			assertThat(keycloak.lastCreateUserBody()).contains("\"username\":\"ada@example.com\"");
			assertThat(keycloak.lastPasswordGrantBody()).contains("client_id=janus-app");
		}
	}

	@Test
	@DisplayName("should use custom AuthenticationPolicy when present")
	void shouldUseCustomAuthenticationPolicyWhenPresent()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, FunctionBeansConfiguration.class,
					CustomPolicyConfiguration.class);
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
			final SpringTestFixtures.RecordingFunctionHandlers handlers =
					context.getBean(SpringTestFixtures.RecordingFunctionHandlers.class);
			handlers.lookupResult = Optional.empty();

			assertThat(authenticationService.signin(SpringTestFixtures.signinCommand()))
					.isInstanceOf(SigninSuccess.class)
					.extracting(SigninSuccess.class::cast)
					.satisfies(signinSuccess -> assertThat(signinSuccess.localAccount()).isEmpty());
		}
	}

	@Test
	@DisplayName("should default AuthenticationPolicy when no policy bean exists")
	void shouldDefaultAuthenticationPolicyWhenNoPolicyBeanExists()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, FunctionBeansConfiguration.class);
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
			final SpringTestFixtures.RecordingFunctionHandlers handlers =
					context.getBean(SpringTestFixtures.RecordingFunctionHandlers.class);
			handlers.lookupResult = Optional.empty();

			assertThat(authenticationService.signin(SpringTestFixtures.signinCommand()))
					.isInstanceOf(SigninFailure.class)
					.extracting(SigninFailure.class::cast)
					.extracting(SigninFailure::reason)
					.isEqualTo(SigninFailureReason.LOCAL_ACCOUNT_NOT_LINKED);
		}
	}

	@Test
	@DisplayName("should default clock to system UTC when no Clock bean exists")
	void shouldDefaultClockToSystemUtcWhenNoClockBeanExists()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, ExplicitPortsConfiguration.class);
			context.refresh();

			assertThat(SpringTestFixtures.extractClock(context.getBean(AuthenticationService.class)).getZone())
					.isEqualTo(ZoneOffset.UTC);
		}
	}

	@Test
	@DisplayName("should use custom Clock bean when present")
	void shouldUseCustomClockBeanWhenPresent()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class,
					ExplicitPortsConfiguration.class,
					CustomClockConfiguration.class);
			context.refresh();

			assertThat(SpringTestFixtures.extractClock(context.getBean(AuthenticationService.class)))
					.isEqualTo(context.getBean(Clock.class));
		}
	}

	@Test
	@DisplayName("should fail clearly when a required collaborator is missing")
	void shouldFailClearlyWhenARequiredCollaboratorIsMissing()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, MissingCreationConfiguration.class);

			assertThatThrownBy(context::refresh)
					.isInstanceOf(BeanCreationException.class)
					.hasMessageContaining("LocalAccountCreationPort");
		}
	}

	@Test
	@DisplayName("should prefer explicit IdentityProviderPort over supported identity provider configuration")
	void shouldPreferExplicitIdentityProviderPortOverSupportedIdentityProviderConfiguration()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
		{
			context.register(JanusAuthenticationSpringConfiguration.class, ExplicitPortsConfiguration.class);
			context.registerBean(JanusSupportedIdentityProvider.class, () -> JanusSupportedIdentityProvider.KEYCLOAK);
			context.registerBean(KeycloakIdentityProviderConfiguration.class, () ->
					KeycloakIdentityProviderConfiguration.of("http://127.0.0.1:65535",
							"janus",
							"janus-app",
							"janus-admin"));
			context.refresh();

			final AuthenticationService authenticationService = context.getBean(AuthenticationService.class);
			final SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort =
					context.getBean(SpringTestFixtures.RecordingIdentityProviderPort.class);

			assertThat(authenticationService.signup(SpringTestFixtures.signupCommand()))
					.isInstanceOf(SignupSuccess.class)
					.extracting(SignupSuccess.class::cast)
					.satisfies(signupSuccess -> assertThat(signupSuccess.providerIdentity().metadata())
							.containsEntry("source", "explicit-provider"));
			assertThat(identityProviderPort.signupCalls).isEqualTo(1);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ExplicitPortsConfiguration
	{
		@Bean
		SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort()
		{
			return new SpringTestFixtures.RecordingIdentityProviderPort();
		}

		@Bean
		SpringTestFixtures.RecordingLocalAccountLookupPort localAccountLookupPort()
		{
			return new SpringTestFixtures.RecordingLocalAccountLookupPort();
		}

		@Bean
		SpringTestFixtures.RecordingLocalAccountCreationPort localAccountCreationPort()
		{
			return new SpringTestFixtures.RecordingLocalAccountCreationPort();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class FunctionBeansConfiguration
	{
		@Bean
		SpringTestFixtures.RecordingFunctionHandlers recordingFunctionHandlers()
		{
			return new SpringTestFixtures.RecordingFunctionHandlers();
		}

		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ ->
			{
				handlers.providerSignupCalls++;
				return handlers.providerSignupResult;
			};
		}

		@Bean
		Function<SigninProviderCommand, ProviderSigninResult> signinProviderFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ ->
			{
				handlers.providerSigninCalls++;
				return handlers.providerSigninResult;
			};
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookupBiFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return (_, _) ->
			{
				handlers.lookupCalls++;
				return handlers.lookupResult;
			};
		}

		@Bean
		Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreationFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ ->
			{
				handlers.creationCalls++;
				return handlers.creationResult;
			};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomPolicyConfiguration
	{
		@Bean
		AuthenticationPolicy authenticationPolicy()
		{
			return AuthenticationPolicy.allowingUnlinkedLocalSignin();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClockConfiguration
	{
		@Bean
		Clock clock()
		{
			return Clock.fixed(Instant.parse("2026-04-15T14:00:00Z"), ZoneOffset.ofHours(2));
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MissingCreationConfiguration
	{
		@Bean
		SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort()
		{
			return new SpringTestFixtures.RecordingIdentityProviderPort();
		}

		@Bean
		SpringTestFixtures.RecordingLocalAccountLookupPort localAccountLookupPort()
		{
			return new SpringTestFixtures.RecordingLocalAccountLookupPort();
		}
	}
}
