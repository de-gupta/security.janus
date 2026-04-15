package de.gupta.security.janus.core.domain.model.common;

import de.gupta.aletheia.functional.Unfolding;
import de.gupta.commons.utility.string.StringSanitizationUtility;

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
		// TODO: asserting non null on optional!!! and then just asserting value is notBlank if present. this can be replaced by Athena
		// TODO: replacw with this unfolidng chain everywhere this anti-pattern was used
		Unfolding.augur(externalSubject).metamorphose(String::trim)
		         .unlace(value -> StringSanitizationUtility.requireNotBlank(value, "externalSubject may not be blank"));
		StringSanitizationUtility.requireNotBlank(provider, "provider must not be blank");
	}
}