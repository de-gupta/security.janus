package de.gupta.security.janus.support;

import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.common.ProviderSession;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class TestFixtures
{
	public static SignupProfileAttributes signupProfileAttributes()
	{
		return SignupProfileAttributes.of(Optional.of(" ada@example.com "),
				Optional.of(" Ada "),
				Optional.of(" Lovelace "),
				Optional.of(" Enchantress of Numbers "));
	}

	public static SignupCommand signupCommand()
	{
		return SignupCommand.of("ada@example.com", "correct horse battery staple", signupProfileAttributes(), "local");
	}

	public static SigninCommand signinCommand()
	{
		return SigninCommand.of("ada@example.com", "correct horse battery staple", "local");
	}

	public static ProviderIdentity providerIdentity()
	{
		return ProviderIdentity.of("provider-subject-1", "local", Map.of("tenant", "test"));
	}

	public static ProviderSession providerSession()
	{
		return ProviderSession.of("local",
				Optional.of("session-1"),
				Optional.of("access-1"),
				Optional.of("refresh-1"),
				Optional.of("id-1"),
				Optional.of(Instant.parse("2026-04-15T10:15:30Z")),
				Optional.of(Instant.parse("2026-04-15T11:15:30Z")),
				Map.of("scope", "basic"));
	}

	public static LocalAccountReference localAccountReference()
	{
		return LocalAccountReference.of("local-account-1", Optional.of("provider-subject-1"), "local", true);
	}

	private TestFixtures()
	{
	}
}
