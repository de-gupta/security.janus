package de.gupta.security.janus.api;

import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signup.SignupResult;

public interface AuthenticationService
{
	SignupResult signup(SignupCommand command);

	SigninResult signin(SigninCommand command);
}