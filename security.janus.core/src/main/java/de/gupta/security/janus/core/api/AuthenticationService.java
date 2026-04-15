package de.gupta.security.janus.core.api;

import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.core.domain.model.signup.SignupResult;

public interface AuthenticationService
{
	SignupResult signup(SignupCommand command);

	SigninResult signin(SigninCommand command);
}