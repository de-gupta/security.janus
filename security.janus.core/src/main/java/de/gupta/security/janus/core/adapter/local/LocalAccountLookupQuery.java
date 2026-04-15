package de.gupta.security.janus.core.adapter.local;

import de.gupta.commons.utility.string.StringSanitizationUtility;

public record LocalAccountLookupQuery(String provider, String externalSubject)
{
	public static LocalAccountLookupQuery of(final String provider, final String externalSubject)
	{
		return new LocalAccountLookupQuery(provider, externalSubject);
	}

	public LocalAccountLookupQuery
	{
		StringSanitizationUtility.requireNotBlank(provider, "provider must not be blank");
		StringSanitizationUtility.requireNotBlank(externalSubject, "externalSubject must not be blank");
	}
}
