package de.gupta.security.janus.core.adapter.provider;

import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;

public interface IdentityProviderPort
{
	ProviderSignupResult signup(SignupProviderCommand command);

	ProviderSigninResult signin(SigninProviderCommand command);
}