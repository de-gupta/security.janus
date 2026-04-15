package de.gupta.security.janus.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderIdentity.of(externalSubject, provider)")
class ProviderIdentityOfDefaultMetadataTest
{
	@Test
	@DisplayName("should create an identity with empty metadata")
	void shouldCreateAnIdentityWithEmptyMetadata()
	{
		assertThat(ProviderIdentity.of("provider-subject-1", "local"))
				.as("provider identity with default metadata")
				.extracting(ProviderIdentity::externalSubject,
						ProviderIdentity::provider,
						ProviderIdentity::metadata)
				.containsExactly("provider-subject-1", "local", Map.of());
	}
}
