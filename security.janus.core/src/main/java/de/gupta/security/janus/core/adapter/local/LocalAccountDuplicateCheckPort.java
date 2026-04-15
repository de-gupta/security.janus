package de.gupta.security.janus.core.adapter.local;

public interface LocalAccountDuplicateCheckPort
{
	boolean existsByLoginIdentifier(String loginIdentifier);
}