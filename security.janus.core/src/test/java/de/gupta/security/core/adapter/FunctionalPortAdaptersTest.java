package de.gupta.security.core.adapter;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupSuccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Functional port adapters")
class FunctionalPortAdaptersTest
{
	private static final class TestFixtures
	{
		private static SignupProviderCommand signupProviderCommand()
		{
			return SignupProviderCommand.of("ada@example.com",
					"correct horse battery staple",
					de.gupta.security.janus.core.api.command.SignupProfileAttributes.empty(),
					"local");
		}

		private static SigninProviderCommand signinProviderCommand()
		{
			return SigninProviderCommand.of("ada@example.com", "correct horse battery staple", "local");
		}

		private static de.gupta.security.janus.core.domain.model.common.ProviderIdentity providerIdentity()
		{
			return de.gupta.security.janus.core.domain.model.common.ProviderIdentity.of("provider-subject-1", "local");
		}

		private static de.gupta.security.janus.core.domain.model.common.ProviderSession providerSession()
		{
			return de.gupta.security.janus.core.domain.model.common.ProviderSession.of("local");
		}

		private static de.gupta.security.janus.core.domain.model.common.LocalAccountReference localAccountReference()
		{
			return de.gupta.security.janus.core.domain.model.common.LocalAccountReference.of("local-account-1",
					Optional.of("provider-subject-1"),
					"local",
					true);
		}

		private static LocalAccountCreationCommand localAccountCreationCommand()
		{
			return LocalAccountCreationCommand.of("ada@example.com",
					de.gupta.security.janus.core.api.command.SignupProfileAttributes.empty(),
					"local",
					providerIdentity(),
					Optional.of(providerSession()));
		}

		private static LocalAccountIdentityView localAccountIdentityView()
		{
			return new LocalAccountIdentityViewFixture("local-account-1", Optional.of("provider-subject-1"), "local",
					true);
		}
	}

	private record LocalAccountIdentityViewFixture(String localAccountId,
	                                               Optional<String> externalSubject,
	                                               String provider,
	                                               boolean active) implements LocalAccountIdentityView
	{
	}

	@Nested
	@DisplayName("IdentityProviderPort.of")
	class IdentityProviderPortOf
	{
		@Test
		@DisplayName("should adapt signup and signin functions")
		void shouldAdaptSignupAndSigninFunctions()
		{
			final AtomicReference<SignupProviderCommand> receivedSignup = new AtomicReference<>();
			final AtomicReference<SigninProviderCommand> receivedSignin = new AtomicReference<>();
			final IdentityProviderPort port = IdentityProviderPort.of(command ->
			{
				receivedSignup.set(command);
				return ProviderSignupSuccess.of(TestFixtures.providerIdentity(),
						Optional.of(TestFixtures.providerSession()));
			}, command ->
			{
				receivedSignin.set(command);
				return ProviderSigninSuccess.of(TestFixtures.providerIdentity(), TestFixtures.providerSession());
			});

			assertThat(port.signup(TestFixtures.signupProviderCommand())).isInstanceOf(ProviderSignupSuccess.class);
			assertThat(port.signin(TestFixtures.signinProviderCommand())).isInstanceOf(ProviderSigninSuccess.class);
			assertThat(receivedSignup).hasValue(TestFixtures.signupProviderCommand());
			assertThat(receivedSignin).hasValue(TestFixtures.signinProviderCommand());
		}

		@Test
		@DisplayName("should reject null functions")
		void shouldRejectNullFunctions()
		{
			assertThatThrownBy(() -> IdentityProviderPort.of(null, _ ->
					ProviderSigninSuccess.of(TestFixtures.providerIdentity(), TestFixtures.providerSession())))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("signupHandler must not be null");

			assertThatThrownBy(() -> IdentityProviderPort.of(_ ->
					ProviderSignupSuccess.of(TestFixtures.providerIdentity(),
							Optional.of(TestFixtures.providerSession())), null))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("signinHandler must not be null");
		}
	}

	@Nested
	@DisplayName("LocalAccountLookupPort.of")
	class LocalAccountLookupPortOf
	{
		@Test
		@DisplayName("should adapt a bifunction lookup handler")
		void shouldAdaptABifunctionLookupHandler()
		{
			final AtomicReference<String> receivedProvider = new AtomicReference<>();
			final AtomicReference<String> receivedExternalSubject = new AtomicReference<>();
			final LocalAccountLookupPort port = LocalAccountLookupPort.of((provider, externalSubject) ->
			{
				receivedProvider.set(provider);
				receivedExternalSubject.set(externalSubject);
				return Optional.of(TestFixtures.localAccountIdentityView());
			});

			assertThat(port.findByProviderSubject("local", "provider-subject-1"))
					.contains(TestFixtures.localAccountIdentityView());
			assertThat(receivedProvider).hasValue("local");
			assertThat(receivedExternalSubject).hasValue("provider-subject-1");
		}

		@Test
		@DisplayName("should adapt a function lookup handler")
		void shouldAdaptAFunctionLookupHandler()
		{
			final AtomicReference<LocalAccountLookupQuery> receivedQuery = new AtomicReference<>();
			final LocalAccountLookupPort port = LocalAccountLookupPort.of(query ->
			{
				receivedQuery.set(query);
				return Optional.of(TestFixtures.localAccountIdentityView());
			});

			assertThat(port.findByProviderSubject("local", "provider-subject-1"))
					.contains(TestFixtures.localAccountIdentityView());
			assertThat(receivedQuery).hasValue(LocalAccountLookupQuery.of("local", "provider-subject-1"));
		}

		@Test
		@DisplayName("should reject null handlers")
		void shouldRejectNullHandlers()
		{
			assertThatThrownBy(() -> LocalAccountLookupPort.of(
					(java.util.function.BiFunction<String, String, Optional<LocalAccountIdentityView>>) null))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("lookupHandler must not be null");

			assertThatThrownBy(() -> LocalAccountLookupPort.of(
					(java.util.function.Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>) null))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("lookupHandler must not be null");
		}
	}

	@Nested
	@DisplayName("LocalAccountCreationPort.of")
	class LocalAccountCreationPortOf
	{
		@Test
		@DisplayName("should adapt a creation function")
		void shouldAdaptACreationFunction()
		{
			final AtomicReference<LocalAccountCreationCommand> receivedCommand = new AtomicReference<>();
			final LocalAccountCreationPort port = LocalAccountCreationPort.of(command ->
			{
				receivedCommand.set(command);
				return LocalAccountCreationSuccess.of(TestFixtures.localAccountReference());
			});

			assertThat(port.createLocalAccount(TestFixtures.localAccountCreationCommand()))
					.isEqualTo(LocalAccountCreationSuccess.of(TestFixtures.localAccountReference()));
			assertThat(receivedCommand).hasValue(TestFixtures.localAccountCreationCommand());
		}

		@Test
		@DisplayName("should reject a null creation function")
		void shouldRejectANullCreationFunction()
		{
			assertThatThrownBy(() -> LocalAccountCreationPort.of(null))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("createHandler must not be null");
		}
	}

	@Nested
	@DisplayName("LocalAccountDuplicateCheckPort.of")
	class LocalAccountDuplicateCheckPortOf
	{
		@Test
		@DisplayName("should adapt a predicate")
		void shouldAdaptAPredicate()
		{
			final AtomicReference<String> receivedLoginIdentifier = new AtomicReference<>();
			final LocalAccountDuplicateCheckPort port = LocalAccountDuplicateCheckPort.of(loginIdentifier ->
			{
				receivedLoginIdentifier.set(loginIdentifier);
				return true;
			});

			assertThat(port.existsByLoginIdentifier("ada@example.com")).isTrue();
			assertThat(receivedLoginIdentifier).hasValue("ada@example.com");
		}

		@Test
		@DisplayName("should reject a null predicate")
		void shouldRejectANullPredicate()
		{
			assertThatThrownBy(() -> LocalAccountDuplicateCheckPort.of(null))
					.isInstanceOf(NullPointerException.class)
					.hasMessage("duplicateCheck must not be null");
		}
	}
}