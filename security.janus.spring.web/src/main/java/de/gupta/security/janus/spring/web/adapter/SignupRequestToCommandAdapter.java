package de.gupta.security.janus.spring.web.adapter;

import de.gupta.security.janus.core.api.command.SignupCommand;

@FunctionalInterface
public interface SignupRequestToCommandAdapter<SignupRequest>
{
	SignupCommand adapt(SignupRequest request);
}