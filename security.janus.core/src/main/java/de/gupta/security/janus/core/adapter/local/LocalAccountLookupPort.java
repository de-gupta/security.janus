package de.gupta.security.janus.core.adapter.local;

import java.util.Optional;

public interface LocalAccountLookupPort
{
	Optional<LocalAccountIdentityView> findByProviderSubject(String provider, String externalSubject);
}