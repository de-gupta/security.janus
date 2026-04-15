package de.gupta.security.janus.core.adapter.local;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface LocalAccountLookupPort
{
	Optional<LocalAccountIdentityView> findByProviderSubject(String provider, String externalSubject);

	static LocalAccountLookupPort of(final BiFunction<String, String, Optional<LocalAccountIdentityView>> lookupHandler)
	{
		Objects.requireNonNull(lookupHandler, "lookupHandler must not be null");

		return lookupHandler::apply;
	}

	static LocalAccountLookupPort of(
			final Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>> lookupHandler)
	{
		Objects.requireNonNull(lookupHandler, "lookupHandler must not be null");

		return (provider, externalSubject) -> lookupHandler.apply(
				LocalAccountLookupQuery.of(provider, externalSubject));
	}
}