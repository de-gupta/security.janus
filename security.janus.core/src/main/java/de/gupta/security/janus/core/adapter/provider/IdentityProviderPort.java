package de.gupta.security.janus.core.adapter.provider;

import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;

import java.util.Objects;
import java.util.function.Function;

public interface IdentityProviderPort
{
	ProviderSignupResult signup(SignupProviderCommand command);

	ProviderSigninResult signin(SigninProviderCommand command);

	static IdentityProviderPort of(final Function<SignupProviderCommand, ProviderSignupResult> signupHandler,
	                               final Function<SigninProviderCommand, ProviderSigninResult> signinHandler)
	{
		Objects.requireNonNull(signupHandler, "signupHandler must not be null");
		Objects.requireNonNull(signinHandler, "signinHandler must not be null");

		return new IdentityProviderPort()
		{
			@Override
			public ProviderSignupResult signup(final SignupProviderCommand command)
			{
				return signupHandler.apply(command);
			}

			@Override
			public ProviderSigninResult signin(final SigninProviderCommand command)
			{
				return signinHandler.apply(command);
			}
		};
	}
}
