package de.gupta.security.janus.adapter.local;

public interface LocalSecretHasher
{
	String hash(String rawSecret);

	boolean matches(String rawSecret, String hashedSecret);
}
