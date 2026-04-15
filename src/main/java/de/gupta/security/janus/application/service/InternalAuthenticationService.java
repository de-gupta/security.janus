package de.gupta.security.janus.application.service;

import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signup.SignupResult;

import java.time.Instant;

public interface InternalAuthenticationService
{
	SignupResult signup(SignupCommand command, Instant issuedAt);

	SigninResult signin(SigninCommand command, Instant issuedAt);
}