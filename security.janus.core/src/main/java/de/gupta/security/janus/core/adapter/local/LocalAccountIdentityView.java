package de.gupta.security.janus.core.adapter.local;

import java.util.Optional;

public interface LocalAccountIdentityView
{
	String localAccountId();

	Optional<String> externalSubject();

	String provider();

	boolean active();
}