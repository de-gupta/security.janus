package de.gupta.security.janus.core.facade;

import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.core.domain.model.signup.SignupResult;

public interface AuthenticationFacade
{
	SignupResult signup(SignupCommand command);

	SigninResult signin(SigninCommand command);
}