package de.gupta.security.janus.domain.model.common;

import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Map;
import java.util.Objects;

public record ProviderIdentity(String externalSubject,
                               String provider,
                               Map<String, Object> metadata)
{
	public static ProviderIdentity of(final String externalSubject, final String provider)
	{
		return new ProviderIdentity(externalSubject, provider, Map.of());
	}

	public static ProviderIdentity of(final String externalSubject,
	                                  final String provider,
	                                  final Map<String, Object> metadata)
	{
		return new ProviderIdentity(externalSubject, provider, metadata);
	}

	public ProviderIdentity
	{
		StringSanitizationUtility.requireNotBlank(externalSubject, "externalSubject must not be blank");
		StringSanitizationUtility.requireNotBlank(provider, "provider must not be blank");
		metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata must not be null"));
	}
}