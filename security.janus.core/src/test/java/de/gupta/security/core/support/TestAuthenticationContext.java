package de.gupta.security.core.support;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.AuthenticationConfiguration;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.AuthenticationServiceFactory;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninSuccess;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupSuccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Function;

public final class TestAuthenticationContext
{
	public final RecordingIdentityProviderPort identityProviderPort = new RecordingIdentityProviderPort();
	public final RecordingLocalAccountLookupPort localAccountLookupPort = new RecordingLocalAccountLookupPort();
	public final RecordingLocalAccountCreationPort localAccountCreationPort = new RecordingLocalAccountCreationPort();
	public final RecordingLocalAccountDuplicateCheckPort localAccountDuplicateCheckPort =
			new RecordingLocalAccountDuplicateCheckPort();
	public final Clock clock = Clock.fixed(Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC);

	public AuthenticationService service()
	{
		return service(AuthenticationPolicy.defaults());
	}

	public AuthenticationService service(final AuthenticationPolicy policy)
	{
		return AuthenticationServiceFactory.create(AuthenticationConfiguration.of(identityProviderPort,
				localAccountLookupPort,
				localAccountCreationPort,
				localAccountDuplicateCheckPort,
				policy,
				clock));
	}

	public AuthenticationService serviceWithoutDuplicateCheck()
	{
		return AuthenticationServiceFactory.create(AuthenticationConfiguration.of(identityProviderPort,
				localAccountLookupPort,
				localAccountCreationPort,
				clock));
	}

	public static final class RecordingIdentityProviderPort implements IdentityProviderPort
	{
		public SignupProviderCommand receivedSignupCommand;
		public SigninProviderCommand receivedSigninCommand;
		public Function<SignupProviderCommand, ProviderSignupResult> signupHandler = _ ->
				ProviderSignupSuccess.of(TestFixtures.providerIdentity(), Optional.of(TestFixtures.providerSession()));
		public Function<SigninProviderCommand, ProviderSigninResult> signinHandler = _ ->
				ProviderSigninSuccess.of(TestFixtures.providerIdentity(), TestFixtures.providerSession());

		@Override
		public ProviderSignupResult signup(final SignupProviderCommand command)
		{
			receivedSignupCommand = command;
			return signupHandler.apply(command);
		}

		@Override
		public ProviderSigninResult signin(final SigninProviderCommand command)
		{
			receivedSigninCommand = command;
			return signinHandler.apply(command);
		}
	}

	public static final class RecordingLocalAccountLookupPort implements LocalAccountLookupPort
	{
		public String receivedProvider;
		public String receivedExternalSubject;
		public Function<LookupKey, Optional<LocalAccountIdentityView>> lookupHandler = key ->
				Optional.of(new LocalAccountIdentityViewStub("local-account-1",
						Optional.of(key.externalSubject()),
						key.provider(),
						true));

		@Override
		public Optional<LocalAccountIdentityView> findByProviderSubject(final String provider,
		                                                                final String externalSubject)
		{
			receivedProvider = provider;
			receivedExternalSubject = externalSubject;
			return lookupHandler.apply(new LookupKey(provider, externalSubject));
		}
	}

	public static final class RecordingLocalAccountCreationPort implements LocalAccountCreationPort
	{
		public LocalAccountCreationCommand receivedCommand;
		public Function<LocalAccountCreationCommand, LocalAccountCreationResult> createHandler = _ ->
				LocalAccountCreationSuccess.of(TestFixtures.localAccountReference());

		@Override
		public LocalAccountCreationResult createLocalAccount(final LocalAccountCreationCommand command)
		{
			receivedCommand = command;
			return createHandler.apply(command);
		}
	}

	public static final class RecordingLocalAccountDuplicateCheckPort implements LocalAccountDuplicateCheckPort
	{
		public String receivedLoginIdentifier;
		public Function<String, Boolean> duplicateHandler = ignored -> false;

		@Override
		public boolean existsByLoginIdentifier(final String loginIdentifier)
		{
			receivedLoginIdentifier = loginIdentifier;
			return duplicateHandler.apply(loginIdentifier);
		}
	}

	public record LookupKey(String provider, String externalSubject)
	{
	}

	public record LocalAccountIdentityViewStub(String localAccountId,
	                                           Optional<String> externalSubject,
	                                           String provider,
	                                           boolean active) implements LocalAccountIdentityView
	{
	}
}