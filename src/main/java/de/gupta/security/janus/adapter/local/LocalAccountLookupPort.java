package de.gupta.security.janus.adapter.local;

import java.util.Optional;

public interface LocalAccountLookupPort
{
	Optional<LocalAccountIdentityView> findByProviderSubject(String provider, String externalSubject);
}
