package de.gupta.security.janus.spring.configuration;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.core.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.core.domain.model.signup.SignupFailure;
import de.gupta.security.janus.core.domain.model.signup.SignupFailureReason;
import de.gupta.security.janus.core.domain.model.signup.SignupSuccess;
import de.gupta.security.janus.spring.support.SpringTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JanusAuthenticationAutoConfiguration")
class JanusAuthenticationAutoConfigurationTest
{
	private final ApplicationContextRunner contextRunner =
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
					JanusAuthenticationAutoConfiguration.class));

	@Test
	@DisplayName("should create AuthenticationService when required collaborators are present")
	void shouldCreateAuthenticationServiceWhenRequiredCollaboratorsArePresent()
	{
		contextRunner.withUserConfiguration(FunctionBeansConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasSingleBean(AuthenticationService.class);
						 assertThat(context.getBean(AuthenticationService.class)
			                               .signup(SpringTestFixtures.signupCommand()))
								 .isInstanceOf(SignupSuccess.class);
					 });
	}

	@Test
	@DisplayName("should back off when user provides AuthenticationService bean")
	void shouldBackOffWhenUserProvidesAuthenticationServiceBean()
	{
		contextRunner.withUserConfiguration(FunctionBeansConfiguration.class,
							 UserAuthenticationServiceConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasSingleBean(AuthenticationService.class);
						 assertThat(context.getBean(AuthenticationService.class))
								 .isSameAs(context.getBean("customAuthenticationService"));
					 });
	}

	@Test
	@DisplayName("should not create AuthenticationService when required collaborators are missing")
	void shouldNotCreateAuthenticationServiceWhenRequiredCollaboratorsAreMissing()
	{
		contextRunner.withUserConfiguration(MissingCreationConfiguration.class)
		             .run(context -> assertThat(context).doesNotHaveBean(AuthenticationService.class));
	}

	@Test
	@DisplayName("should resolve lookup from LocalAccountLookupQuery function")
	void shouldResolveLookupFromLocalAccountLookupQueryFunction()
	{
		contextRunner.withUserConfiguration(LookupQueryFunctionConfiguration.class)
		             .run(context ->
					 {
						 final AuthenticationService authenticationService =
								 context.getBean(AuthenticationService.class);

						 assertThat(authenticationService.signin(SpringTestFixtures.signinCommand()))
								 .isInstanceOf(SigninSuccess.class)
					             .extracting(SigninSuccess.class::cast)
					             .satisfies(signinSuccess -> assertThat(signinSuccess.localAccount())
										 .get()
							             .extracting(LocalAccountReference::localAccountId)
							             .isEqualTo("query-lookup-local-account"));
					 });
	}

	@Test
	@DisplayName("should prefer explicit ports over function fallbacks")
	void shouldPreferExplicitPortsOverFunctionFallbacks()
	{
		contextRunner.withUserConfiguration(ExplicitPortsWithFallbacksConfiguration.class)
		             .run(context ->
					 {
						 final AuthenticationService authenticationService =
								 context.getBean(AuthenticationService.class);
						 final SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort =
								 context.getBean(SpringTestFixtures.RecordingIdentityProviderPort.class);
						 final SpringTestFixtures.RecordingLocalAccountLookupPort localAccountLookupPort =
								 context.getBean(SpringTestFixtures.RecordingLocalAccountLookupPort.class);
						 final SpringTestFixtures.RecordingLocalAccountCreationPort localAccountCreationPort =
								 context.getBean(SpringTestFixtures.RecordingLocalAccountCreationPort.class);
						 final SpringTestFixtures.RecordingFunctionHandlers handlers =
								 context.getBean(SpringTestFixtures.RecordingFunctionHandlers.class);

						 assertThat(authenticationService.signup(SpringTestFixtures.signupCommand()))
								 .isInstanceOf(SignupSuccess.class)
					             .extracting(SignupSuccess.class::cast)
					             .satisfies(signupSuccess ->
								 {
									 assertThat(signupSuccess.providerIdentity().metadata())
											 .containsEntry("source", "explicit-provider");
									 assertThat(signupSuccess.localAccount().localAccountId())
											 .isEqualTo("explicit-creation-local-account");
								 });
						 assertThat(authenticationService.signin(SpringTestFixtures.signinCommand()))
								 .isInstanceOf(SigninSuccess.class)
					             .extracting(SigninSuccess.class::cast)
					             .satisfies(signinSuccess -> assertThat(signinSuccess.localAccount())
										 .get()
							             .extracting(LocalAccountReference::localAccountId)
							             .isEqualTo("explicit-lookup-local-account"));

						 assertThat(identityProviderPort.signupCalls).isEqualTo(1);
						 assertThat(identityProviderPort.signinCalls).isEqualTo(1);
						 assertThat(localAccountCreationPort.calls).isEqualTo(1);
						 assertThat(localAccountLookupPort.calls).isEqualTo(1);
						 assertThat(handlers.providerSignupCalls).isZero();
						 assertThat(handlers.providerSigninCalls).isZero();
						 assertThat(handlers.creationCalls).isZero();
						 assertThat(handlers.lookupCalls).isZero();
					 });
	}

	@Test
	@DisplayName("should prefer explicit duplicate check port over predicate fallback")
	void shouldPreferExplicitDuplicateCheckPortOverPredicateFallback()
	{
		contextRunner.withUserConfiguration(ExplicitDuplicateCheckWithPredicateConfiguration.class)
		             .run(context ->
					 {
						 final AuthenticationService authenticationService =
								 context.getBean(AuthenticationService.class);
						 final SpringTestFixtures.RecordingIdentityProviderPort identityProviderPort =
								 context.getBean(SpringTestFixtures.RecordingIdentityProviderPort.class);

						 assertThat(authenticationService.signup(SpringTestFixtures.signupCommand()))
								 .isInstanceOf(SignupFailure.class)
					             .extracting(SignupFailure.class::cast)
					             .extracting(SignupFailure::reason)
					             .isEqualTo(SignupFailureReason.DUPLICATE_LOCAL_ACCOUNT);
						 assertThat(identityProviderPort.signupCalls).isZero();
					 });
	}

	@Test
	@DisplayName("should fail clearly when both lookup fallback shapes are present")
	void shouldFailClearlyWhenBothLookupFallbackShapesArePresent()
	{
		contextRunner.withUserConfiguration(BothLookupFallbackShapesConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasFailed();
						 assertThat(context.getStartupFailure()).hasMessageContaining(
								 "Both lookup fallback shapes are present");
					 });
	}

	@Test
	@DisplayName("should fail clearly when multiple lookup BiFunction beans are present")
	void shouldFailClearlyWhenMultipleLookupBiFunctionBeansArePresent()
	{
		contextRunner.withUserConfiguration(AmbiguousLookupBiFunctionConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasFailed();
						 assertThat(context.getStartupFailure())
								 .hasMessageContaining(
										 "BiFunction<String, String, Optional<LocalAccountIdentityView>>");
					 });
	}

	@Test
	@DisplayName("should fail clearly when multiple explicit port beans are present")
	void shouldFailClearlyWhenMultipleExplicitPortBeansArePresent()
	{
		contextRunner.withUserConfiguration(AmbiguousIdentityProviderPortConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasFailed();
						 assertThat(context.getStartupFailure()).hasMessageContaining("IdentityProviderPort");
					 });
	}

	@Test
	@DisplayName("should fail clearly when provider fallback is incomplete")
	void shouldFailClearlyWhenProviderFallbackIsIncomplete()
	{
		contextRunner.withUserConfiguration(IncompleteProviderFallbackConfiguration.class)
		             .run(context ->
					 {
						 assertThat(context).hasFailed();
						 assertThat(context.getStartupFailure())
								 .hasMessageContaining(
										 "must provide both Function<SignupProviderCommand, ProviderSignupResult> and Function<SigninProviderCommand, ProviderSigninResult>");
					 });
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
	static class LookupQueryFunctionConfiguration
	{
		@Bean
		SpringTestFixtures.RecordingFunctionHandlers recordingFunctionHandlers()
		{
			final SpringTestFixtures.RecordingFunctionHandlers handlers =
					new SpringTestFixtures.RecordingFunctionHandlers();
			handlers.lookupResult = Optional.of(SpringTestFixtures.localAccountIdentityView("query-lookup"));
			return handlers;
		}

		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ -> handlers.providerSignupResult;
		}

		@Bean
		Function<SigninProviderCommand, ProviderSigninResult> signinProviderFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ -> handlers.providerSigninResult;
		}

		@Bean
		Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>> localAccountLookupQueryFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ -> handlers.lookupResult;
		}

		@Bean
		Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreationFunction(
				final SpringTestFixtures.RecordingFunctionHandlers handlers)
		{
			return _ -> handlers.creationResult;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class UserAuthenticationServiceConfiguration
	{
		@Bean
		AuthenticationService customAuthenticationService()
		{
			return new AuthenticationService()
			{
				@Override
				public de.gupta.security.janus.core.domain.model.signup.SignupResult signup(
						final de.gupta.security.janus.core.api.command.SignupCommand command)
				{
					throw new UnsupportedOperationException("not used");
				}

				@Override
				public de.gupta.security.janus.core.domain.model.signin.SigninResult signin(
						final de.gupta.security.janus.core.api.command.SigninCommand command)
				{
					throw new UnsupportedOperationException("not used");
				}
			};
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class MissingCreationConfiguration
	{
		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signupResult;
		}

		@Bean
		Function<SigninProviderCommand, ProviderSigninResult> signinProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signinResult;
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookupBiFunction()
		{
			return (_, _) -> Optional.of(
					SpringTestFixtures.localAccountIdentityView("missing-creation"));
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ExplicitPortsWithFallbacksConfiguration
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
	static class ExplicitDuplicateCheckWithPredicateConfiguration
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

		@Bean
		SpringTestFixtures.RecordingLocalAccountDuplicateCheckPort localAccountDuplicateCheckPort()
		{
			final SpringTestFixtures.RecordingLocalAccountDuplicateCheckPort port =
					new SpringTestFixtures.RecordingLocalAccountDuplicateCheckPort();
			port.duplicate = true;
			return port;
		}

		@Bean
		Predicate<String> duplicateCheckPredicate()
		{
			return _ -> false;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class BothLookupFallbackShapesConfiguration
	{
		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signupResult;
		}

		@Bean
		Function<SigninProviderCommand, ProviderSigninResult> signinProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signinResult;
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookupBiFunction()
		{
			return (_, _) -> Optional.of(SpringTestFixtures.localAccountIdentityView("bi"));
		}

		@Bean
		Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>> localAccountLookupQueryFunction()
		{
			return _ -> Optional.of(SpringTestFixtures.localAccountIdentityView("query"));
		}

		@Bean
		Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreationFunction()
		{
			return _ -> new SpringTestFixtures.RecordingLocalAccountCreationPort().response;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class AmbiguousLookupBiFunctionConfiguration
	{
		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signupResult;
		}

		@Bean
		Function<SigninProviderCommand, ProviderSigninResult> signinProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signinResult;
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> firstLookupBiFunction()
		{
			return (_, _) -> Optional.of(SpringTestFixtures.localAccountIdentityView("first"));
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> secondLookupBiFunction()
		{
			return (_, _) -> Optional.of(SpringTestFixtures.localAccountIdentityView("second"));
		}

		@Bean
		Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreationFunction()
		{
			return _ -> new SpringTestFixtures.RecordingLocalAccountCreationPort().response;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class AmbiguousIdentityProviderPortConfiguration
	{
		@Bean
		IdentityProviderPort firstIdentityProviderPort()
		{
			return new SpringTestFixtures.RecordingIdentityProviderPort();
		}

		@Bean
		IdentityProviderPort secondIdentityProviderPort()
		{
			return new SpringTestFixtures.RecordingIdentityProviderPort();
		}

		@Bean
		LocalAccountLookupPort localAccountLookupPort()
		{
			return new SpringTestFixtures.RecordingLocalAccountLookupPort();
		}

		@Bean
		LocalAccountCreationPort localAccountCreationPort()
		{
			return new SpringTestFixtures.RecordingLocalAccountCreationPort();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class IncompleteProviderFallbackConfiguration
	{
		@Bean
		Function<SignupProviderCommand, ProviderSignupResult> signupProviderFunction()
		{
			return _ -> new SpringTestFixtures.RecordingIdentityProviderPort().signupResult;
		}

		@Bean
		BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookupBiFunction()
		{
			return (_, _) -> Optional.of(SpringTestFixtures.localAccountIdentityView("lookup"));
		}

		@Bean
		Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreationFunction()
		{
			return _ -> new SpringTestFixtures.RecordingLocalAccountCreationPort().response;
		}
	}
}