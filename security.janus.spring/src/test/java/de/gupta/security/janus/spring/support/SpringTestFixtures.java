package de.gupta.security.janus.spring.support;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.api.command.SignupProfileAttributes;
import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupSuccess;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class SpringTestFixtures
{
	public static SignupCommand signupCommand()
	{
		return signupCommand("local");
	}

	public static SigninCommand signinCommand()
	{
		return signinCommand("local");
	}

	public static SignupCommand signupCommand(final String requestedProvider)
	{
		return SignupCommand.of("ada@example.com",
				"correct horse battery staple",
				SignupProfileAttributes.of(Optional.of("ada@example.com"),
						Optional.of("Ada"),
						Optional.of("Lovelace"),
						Optional.of("Ada Lovelace")),
				requestedProvider);
	}

	public static SigninCommand signinCommand(final String requestedProvider)
	{
		return SigninCommand.of("ada@example.com", "correct horse battery staple", requestedProvider);
	}

	public static ProviderIdentity providerIdentity(final String source)
	{
		return ProviderIdentity.of(source + "-subject", "local", Map.of("source", source));
	}

	public static ProviderSession providerSession(final String source)
	{
		return ProviderSession.of("local",
				Optional.of(source + "-session"),
				Optional.of(source + "-access"),
				Optional.of(source + "-refresh"),
				Optional.of(source + "-id-token"),
				Optional.of(Instant.parse("2026-04-15T10:15:30Z")),
				Optional.of(Instant.parse("2026-04-15T11:15:30Z")),
				Map.of("source", source));
	}

	public static LocalAccountReference localAccountReference(final String source)
	{
		return LocalAccountReference.of(source + "-local-account",
				Optional.of(source + "-subject"),
				"local",
				true);
	}

	public static LocalAccountIdentityView localAccountIdentityView(final String source)
	{
		return new LocalAccountIdentityViewFixture(source + "-local-account",
				Optional.of(source + "-subject"),
				"local",
				true);
	}

	public static Clock extractClock(final AuthenticationService authenticationService)
	{
		try
		{
			final Field facadeField = authenticationService.getClass().getDeclaredField("facade");
			facadeField.setAccessible(true);
			final Object facade = facadeField.get(authenticationService);

			final Field clockField = facade.getClass().getDeclaredField("clock");
			clockField.setAccessible(true);
			return (Clock) clockField.get(facade);
		}
		catch (final ReflectiveOperationException exception)
		{
			throw new IllegalStateException("Unable to inspect clock from AuthenticationService", exception);
		}
	}

	private SpringTestFixtures()
	{
	}

	public static final class RecordingIdentityProviderPort implements IdentityProviderPort
	{
		public int signupCalls;
		public int signinCalls;
		public ProviderSignupResult signupResult = ProviderSignupSuccess.of(providerIdentity("explicit-provider"),
				Optional.of(providerSession("explicit-provider")));
		public ProviderSigninResult signinResult = ProviderSigninSuccess.of(providerIdentity("explicit-provider"),
				providerSession("explicit-provider"));

		@Override
		public ProviderSignupResult signup(final SignupProviderCommand command)
		{
			signupCalls++;
			return signupResult;
		}

		@Override
		public ProviderSigninResult signin(final SigninProviderCommand command)
		{
			signinCalls++;
			return signinResult;
		}
	}

	public static final class RecordingLocalAccountLookupPort implements LocalAccountLookupPort
	{
		public int calls;
		public Optional<LocalAccountIdentityView> response =
				Optional.of(localAccountIdentityView("explicit-lookup"));

		@Override
		public Optional<LocalAccountIdentityView> findByProviderSubject(final String provider,
		                                                                final String externalSubject)
		{
			calls++;
			return response;
		}
	}

	public static final class RecordingLocalAccountCreationPort implements LocalAccountCreationPort
	{
		public int calls;
		public LocalAccountCreationResult response =
				LocalAccountCreationSuccess.of(localAccountReference("explicit-creation"));

		@Override
		public LocalAccountCreationResult createLocalAccount(final LocalAccountCreationCommand command)
		{
			calls++;
			return response;
		}
	}

	public static final class RecordingLocalAccountDuplicateCheckPort implements LocalAccountDuplicateCheckPort
	{
		public int calls;
		public boolean duplicate;

		@Override
		public boolean existsByLoginIdentifier(final String loginIdentifier)
		{
			calls++;
			return duplicate;
		}
	}

	public static final class RecordingFunctionHandlers
	{
		public int providerSignupCalls;
		public int providerSigninCalls;
		public int lookupCalls;
		public int creationCalls;
		public ProviderSignupResult providerSignupResult =
				ProviderSignupSuccess.of(providerIdentity("function-provider"),
						Optional.of(providerSession("function-provider")));
		public ProviderSigninResult providerSigninResult =
				ProviderSigninSuccess.of(providerIdentity("function-provider"),
						providerSession("function-provider"));
		public Optional<LocalAccountIdentityView> lookupResult =
				Optional.of(localAccountIdentityView("function-lookup"));
		public LocalAccountCreationResult creationResult =
				LocalAccountCreationSuccess.of(localAccountReference("function-creation"));
		public boolean duplicate;
	}

	private record LocalAccountIdentityViewFixture(String localAccountId,
	                                               Optional<String> externalSubject,
	                                               String provider,
	                                               boolean active) implements LocalAccountIdentityView
	{
	}
}
