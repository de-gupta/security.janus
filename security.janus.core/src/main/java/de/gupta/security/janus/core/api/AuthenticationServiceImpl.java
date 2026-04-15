package de.gupta.security.janus.core.api;

import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.core.domain.model.signup.SignupResult;
import de.gupta.security.janus.core.facade.AuthenticationFacade;

final class AuthenticationServiceImpl implements AuthenticationService
{
	private final AuthenticationFacade facade;

	static AuthenticationService create(final AuthenticationFacade facade)
	{
		return new AuthenticationServiceImpl(facade);
	}

	@Override
	public SignupResult signup(final SignupCommand command)
	{
		return facade.signup(command);
	}

	@Override
	public SigninResult signin(final SigninCommand command)
	{
		return facade.signin(command);
	}

	private AuthenticationServiceImpl(final AuthenticationFacade facade)
	{
		this.facade = facade;
	}
}