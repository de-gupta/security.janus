package de.gupta.security.janus.core.application.service;

import de.gupta.security.janus.core.api.command.SigninCommand;
import de.gupta.security.janus.core.api.command.SignupCommand;
import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.core.domain.model.signup.SignupResult;

import java.time.Instant;

public interface InternalAuthenticationService
{
	SignupResult signup(SignupCommand command, Instant issuedAt);

	SigninResult signin(SigninCommand command, Instant issuedAt);
}