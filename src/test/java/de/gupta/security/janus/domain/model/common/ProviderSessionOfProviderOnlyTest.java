package de.gupta.security.janus.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderSession.of(provider)")
class ProviderSessionOfProviderOnlyTest
{
	@Test
	@DisplayName("should create an empty provider session for the provider")
	void shouldCreateAnEmptyProviderSessionForTheProvider()
	{
		assertThat(ProviderSession.of("local"))
				.as("provider-only session")
				.extracting(ProviderSession::provider,
						ProviderSession::sessionId,
						ProviderSession::accessToken,
						ProviderSession::refreshToken,
						ProviderSession::idToken,
						ProviderSession::issuedAt,
						ProviderSession::expiresAt,
						ProviderSession::metadata)
				.containsExactly("local",
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Map.of());
	}
}
