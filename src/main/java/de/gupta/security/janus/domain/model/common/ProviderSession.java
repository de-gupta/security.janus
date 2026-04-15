package de.gupta.security.janus.domain.model.common;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ProviderSession(String provider, Optional<String> sessionId, Optional<String> accessToken,
                              Optional<String> refreshToken, Optional<String> idToken, Optional<Instant> issuedAt,
                              Optional<Instant> expiresAt, Map<String, Object> metadata)
{
	public static ProviderSession of(final String provider)
	{
		return new ProviderSession(provider, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty(), Optional.empty(), Map.of());
	}

	public static ProviderSession of(final String provider, final Optional<String> sessionId,
	                                 final Optional<String> accessToken, final Optional<String> refreshToken,
	                                 final Optional<String> idToken, final Optional<Instant> issuedAt,
	                                 final Optional<Instant> expiresAt, final Map<String, Object> metadata)
	{
		return new ProviderSession(provider, sessionId, accessToken, refreshToken, idToken, issuedAt, expiresAt,
				metadata);
	}

	public ProviderSession
	{
		StringSanitizationUtility.requireNotBlank(provider, "provider must not be blank");
		sessionId = sanitizeOptional(sessionId, "sessionId must not be null");
		accessToken = sanitizeOptional(accessToken, "accessToken must not be null");
		refreshToken = sanitizeOptional(refreshToken, "refreshToken must not be null");
		idToken = sanitizeOptional(idToken, "idToken must not be null");
		Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
	}

	private static Optional<String> sanitizeOptional(final Optional<String> value, final String message)
	{
		return Unfolding.augur(value)
		                .metamorphose(String::trim)
		                .unlace(optionalValue -> StringSanitizationUtility.requireNotBlank(optionalValue,
				                "optional string value must not be blank"))
		                .optional();
	}
}
