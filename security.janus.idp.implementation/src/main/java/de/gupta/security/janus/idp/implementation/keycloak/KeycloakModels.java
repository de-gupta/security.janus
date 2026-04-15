package de.gupta.security.janus.idp.implementation.keycloak;

import java.util.Optional;

enum KeycloakFailureCategory
{
	UNSUPPORTED_PROVIDER,
	DUPLICATE_IDENTITY,
	INVALID_CREDENTIALS,
	MALFORMED_COMMAND,
	PROVIDER_REJECTED,
	PROVIDER_UNAVAILABLE,
	INTERNAL_ERROR
}

record KeycloakUserCreationResult(String userId, String username, Optional<String> email)
{
}

record KeycloakTokenResponse(Optional<String> accessToken,
                             Optional<Long> expiresIn,
                             Optional<String> refreshToken,
                             Optional<String> idToken,
                             Optional<String> sessionState,
                             Optional<String> scope,
                             Optional<String> tokenType,
                             Optional<String> subject,
                             Optional<String> preferredUsername,
                             Optional<String> email)
{
}

final class KeycloakClientException extends Exception
{
	private final KeycloakFailureCategory category;

	KeycloakFailureCategory category()
	{
		return category;
	}

	KeycloakClientException(final KeycloakFailureCategory category, final String message)
	{
		super(message);
		this.category = category;
	}

	KeycloakClientException(final KeycloakFailureCategory category, final String message, final Throwable cause)
	{
		super(message, cause);
		this.category = category;
	}
}