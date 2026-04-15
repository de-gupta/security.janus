package de.gupta.security.janus.adapter.local;

public interface LocalAccountDuplicateCheckPort
{
	boolean existsByLoginIdentifier(String loginIdentifier);
}
