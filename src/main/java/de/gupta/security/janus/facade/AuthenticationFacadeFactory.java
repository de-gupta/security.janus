package de.gupta.security.janus.facade;

import de.gupta.security.janus.api.AuthenticationConfiguration;
import de.gupta.security.janus.application.service.InternalAuthenticationService;
import de.gupta.security.janus.application.service.InternalAuthenticationServiceFactory;

public final class AuthenticationFacadeFactory
{
	public static AuthenticationFacade create(final AuthenticationConfiguration configuration)
	{
		final InternalAuthenticationService service = InternalAuthenticationServiceFactory.create(configuration);
		return AuthenticationFacadeImpl.create(service, configuration.clock());
	}

	private AuthenticationFacadeFactory()
	{
	}
}
