package de.gupta.security.janus.spring.web.adapter;

import de.gupta.security.janus.core.api.command.SigninCommand;

@FunctionalInterface
public interface SigninRequestToCommandAdapter<SigninRequest>
{
	SigninCommand adapt(SigninRequest request);
}