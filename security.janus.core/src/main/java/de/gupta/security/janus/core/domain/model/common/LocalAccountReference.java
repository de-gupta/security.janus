package de.gupta.security.janus.core.domain.model.common;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.commons.utility.string.StringSanitizationUtility;

import java.util.Objects;
import java.util.Optional;

public record LocalAccountReference(String localAccountId, Optional<String> externalSubject, String provider,
                                    boolean active)
{
	public static LocalAccountReference of(final String localAccountId, final Optional<String> externalSubject,
	                                       final String provider, final boolean active)
	{
		return new LocalAccountReference(localAccountId, externalSubject, provider, active);
	}

	public LocalAccountReference
	{
		StringSanitizationUtility.requireNotBlank(localAccountId, "localAccountId must not be blank");

		Objects.requireNonNull(externalSubject, "externalSubject must not be null");
		externalSubject = Unfolding.augur(externalSubject)
		                           .unlace(value -> StringSanitizationUtility.requireNotBlank(value,
										   "externalSubject may not be blank"))
		                           .metamorphose(Optional::of)
		                           .infuse(Optional.empty());

		StringSanitizationUtility.requireNotBlank(provider, "provider must not be blank");
	}
}