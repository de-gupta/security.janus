package de.gupta.security.core.domain.model.common;

import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
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