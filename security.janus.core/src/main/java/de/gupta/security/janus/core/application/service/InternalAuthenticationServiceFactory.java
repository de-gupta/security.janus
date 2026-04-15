package de.gupta.security.janus.core.application.service;

import de.gupta.security.janus.core.api.AuthenticationConfiguration;

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