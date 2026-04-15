package de.gupta.security.janus.adapter.provider;

import de.gupta.security.janus.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.domain.model.provider.ProviderSignupResult;

public interface IdentityProviderPort
{
	ProviderSignupResult signup(SignupProviderCommand command);

	ProviderSigninResult signin(SigninProviderCommand command);
}