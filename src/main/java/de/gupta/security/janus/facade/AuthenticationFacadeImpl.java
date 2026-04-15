package de.gupta.security.janus.facade;

import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.application.service.InternalAuthenticationService;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signup.SignupResult;

import java.time.Clock;

final class AuthenticationFacadeImpl implements AuthenticationFacade
{
	private final InternalAuthenticationService service;
	private final Clock clock;

	static AuthenticationFacade create(final InternalAuthenticationService service, final Clock clock)
	{
		return new AuthenticationFacadeImpl(service, clock);
	}

	@Override
	public SignupResult signup(final SignupCommand command)
	{
		return service.signup(command, clock.instant());
	}

	@Override
	public SigninResult signin(final SigninCommand command)
	{
		return service.signin(command, clock.instant());
	}

	private AuthenticationFacadeImpl(final InternalAuthenticationService service, final Clock clock)
	{
		this.service = service;
		this.clock = clock;
	}
}