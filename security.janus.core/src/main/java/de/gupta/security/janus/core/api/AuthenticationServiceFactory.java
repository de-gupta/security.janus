package de.gupta.security.janus.core.api;

import de.gupta.security.janus.core.facade.AuthenticationFacadeFactory;

import java.util.Objects;

public final class AuthenticationServiceFactory
{
	public static AuthenticationService create(final AuthenticationConfiguration configuration)
	{
		Objects.requireNonNull(configuration, "configuration must not be null");

		return AuthenticationServiceImpl.create(AuthenticationFacadeFactory.create(configuration));
	}

	private AuthenticationServiceFactory()
	{
	}
}