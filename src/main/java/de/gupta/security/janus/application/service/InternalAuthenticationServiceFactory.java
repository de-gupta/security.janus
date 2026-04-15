package de.gupta.security.janus.application.service;

import de.gupta.security.janus.api.AuthenticationConfiguration;

import java.util.Objects;

public final class InternalAuthenticationServiceFactory
{
	public static InternalAuthenticationService create(final AuthenticationConfiguration configuration)
	{
		return new InternalAuthenticationServiceImpl(configuration);
	}

	private InternalAuthenticationServiceFactory()
	{
	}
}