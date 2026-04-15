package de.gupta.security.janus.idp.implementation.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.core.domain.model.common.ProviderSession;
import de.gupta.security.janus.core.domain.model.provider.*;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class KeycloakIdentityProviderPort implements IdentityProviderPort
{
	private final KeycloakIdentityProviderConfiguration configuration;
	private final KeycloakApiClient keycloakApiClient;

	public static KeycloakIdentityProviderPort create(final KeycloakIdentityProviderConfiguration configuration)
	{
		final HttpClient httpClient = HttpClient.newBuilder()
		                                        .connectTimeout(configuration.connectTimeout())
		                                        .build();
		return new KeycloakIdentityProviderPort(configuration,
				new KeycloakApiClient(httpClient, new ObjectMapper(), configuration));
	}

	@Override
	public ProviderSignupResult signup(final SignupProviderCommand command)
	{
		if (!supports(command.requestedProvider()))
		{
			return ProviderSignupFailure.of(ProviderSignupFailureReason.UNSUPPORTED_PROVIDER,
					"requested provider is not supported by this Keycloak adapter");
		}

		try
		{
			final KeycloakUserCreationResult createdUser = keycloakApiClient.createUser(command);
			keycloakApiClient.setPassword(createdUser.userId(), command.rawSecret());

			final Optional<ProviderSession> providerSession = configuration.signInAfterSignup()
					? signInForSignup(command.loginIdentifier(), command.rawSecret())
					: Optional.empty();

			return ProviderSignupSuccess.of(toProviderIdentity(createdUser), providerSession);
		}
		catch (final KeycloakClientException exception)
		{
			return mapSignupFailure(exception);
		}
	}

	@Override
	public ProviderSigninResult signin(final SigninProviderCommand command)
	{
		if (!supports(command.requestedProvider()))
		{
			return ProviderSigninFailure.of(ProviderSigninFailureReason.UNSUPPORTED_PROVIDER,
					"requested provider is not supported by this Keycloak adapter");
		}

		try
		{
			final KeycloakTokenResponse tokenResponse = keycloakApiClient.passwordGrant(command.loginIdentifier(),
					command.rawSecret());

			return ProviderSigninSuccess.of(toProviderIdentity(tokenResponse),
					toProviderSession(tokenResponse));
		}
		catch (final KeycloakClientException exception)
		{
			return mapSigninFailure(exception);
		}
	}

	private Optional<ProviderSession> signInForSignup(final String loginIdentifier, final String rawSecret)
	{
		try
		{
			return Optional.of(toProviderSession(keycloakApiClient.passwordGrant(loginIdentifier, rawSecret)));
		}
		catch (final KeycloakClientException exception)
		{
			return Optional.empty();
		}
	}

	private ProviderSignupFailure mapSignupFailure(final KeycloakClientException exception)
	{
		return switch (exception.category())
		{
			case UNSUPPORTED_PROVIDER -> ProviderSignupFailure.of(ProviderSignupFailureReason.UNSUPPORTED_PROVIDER,
					exception.getMessage());
			case DUPLICATE_IDENTITY -> ProviderSignupFailure.of(ProviderSignupFailureReason.DUPLICATE_IDENTITY,
					exception.getMessage());
			case MALFORMED_COMMAND -> ProviderSignupFailure.of(ProviderSignupFailureReason.MALFORMED_COMMAND,
					exception.getMessage());
			case PROVIDER_REJECTED -> ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_REJECTED,
					exception.getMessage());
			case PROVIDER_UNAVAILABLE -> ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_UNAVAILABLE,
					exception.getMessage());
			case INTERNAL_ERROR -> ProviderSignupFailure.of(ProviderSignupFailureReason.INTERNAL_ERROR,
					exception.getMessage());
			case INVALID_CREDENTIALS -> ProviderSignupFailure.of(ProviderSignupFailureReason.PROVIDER_REJECTED,
					exception.getMessage());
		};
	}

	private ProviderSigninFailure mapSigninFailure(final KeycloakClientException exception)
	{
		return switch (exception.category())
		{
			case UNSUPPORTED_PROVIDER -> ProviderSigninFailure.of(ProviderSigninFailureReason.UNSUPPORTED_PROVIDER,
					exception.getMessage());
			case INVALID_CREDENTIALS -> ProviderSigninFailure.of(ProviderSigninFailureReason.INVALID_CREDENTIALS,
					exception.getMessage());
			case PROVIDER_REJECTED -> ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_REJECTED,
					exception.getMessage());
			case PROVIDER_UNAVAILABLE -> ProviderSigninFailure.of(ProviderSigninFailureReason.PROVIDER_UNAVAILABLE,
					exception.getMessage());
			case MALFORMED_COMMAND -> ProviderSigninFailure.of(ProviderSigninFailureReason.MALFORMED_COMMAND,
					exception.getMessage());
			case INTERNAL_ERROR, DUPLICATE_IDENTITY ->
					ProviderSigninFailure.of(ProviderSigninFailureReason.INTERNAL_ERROR,
							exception.getMessage());
		};
	}

	private ProviderIdentity toProviderIdentity(final KeycloakUserCreationResult createdUser)
	{
		final Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("realm", configuration.realm());
		metadata.put("username", createdUser.username());
		createdUser.email().ifPresent(email -> metadata.put("email", email));

		return ProviderIdentity.of(createdUser.userId(), configuration.providerAlias(), metadata);
	}

	private ProviderIdentity toProviderIdentity(final KeycloakTokenResponse tokenResponse)
			throws KeycloakClientException
	{
		final String subject = tokenResponse.subject()
		                                    .orElseThrow(() -> new KeycloakClientException(
													KeycloakFailureCategory.INTERNAL_ERROR,
													"Keycloak token response did not include a subject"));

		final Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("realm", configuration.realm());
		tokenResponse.preferredUsername().ifPresent(username -> metadata.put("username", username));
		tokenResponse.email().ifPresent(email -> metadata.put("email", email));

		return ProviderIdentity.of(subject, configuration.providerAlias(), metadata);
	}

	private ProviderSession toProviderSession(final KeycloakTokenResponse tokenResponse)
	{
		final Instant issuedAt = configuration.clock().instant();
		final Optional<Instant> expiresAt = tokenResponse.expiresIn()
		                                                 .map(issuedAt::plusSeconds);
		final Map<String, Object> metadata = new LinkedHashMap<>();
		tokenResponse.scope().ifPresent(scope -> metadata.put("scope", scope));
		tokenResponse.tokenType().ifPresent(tokenType -> metadata.put("tokenType", tokenType));

		return ProviderSession.of(configuration.providerAlias(),
				tokenResponse.sessionState(),
				tokenResponse.accessToken(),
				tokenResponse.refreshToken(),
				tokenResponse.idToken(),
				Optional.of(issuedAt),
				expiresAt,
				metadata);
	}

	private boolean supports(final String requestedProvider)
	{
		return configuration.providerAlias().equals(requestedProvider);
	}

	KeycloakIdentityProviderPort(final KeycloakIdentityProviderConfiguration configuration,
	                             final KeycloakApiClient keycloakApiClient)
	{
		this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
		this.keycloakApiClient = Objects.requireNonNull(keycloakApiClient, "keycloakApiClient must not be null");
	}
}