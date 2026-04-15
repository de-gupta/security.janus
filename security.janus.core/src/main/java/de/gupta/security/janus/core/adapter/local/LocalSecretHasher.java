package de.gupta.security.janus.core.adapter.local;

public interface LocalSecretHasher
{
	String hash(String rawSecret);

	boolean matches(String rawSecret, String hashedSecret);
}