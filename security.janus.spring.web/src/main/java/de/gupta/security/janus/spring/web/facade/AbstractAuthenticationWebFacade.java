package de.gupta.security.janus.spring.web.facade;

import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.spring.web.adapter.SigninRequestToCommandAdapter;
import de.gupta.security.janus.spring.web.adapter.SigninResultToWebResponseAdapter;
import de.gupta.security.janus.spring.web.adapter.SignupRequestToCommandAdapter;
import de.gupta.security.janus.spring.web.adapter.SignupResultToWebResponseAdapter;

import java.util.Objects;

public abstract class AbstractAuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody>
		implements AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody>
{
	private final AuthenticationService authenticationService;
	private final SignupRequestToCommandAdapter<SignupRequest> signupRequestAdapter;
	private final SigninRequestToCommandAdapter<SigninRequest> signinRequestAdapter;
	private final SignupResultToWebResponseAdapter<SignupBody> signupResponseAdapter;
	private final SigninResultToWebResponseAdapter<SigninBody> signinResponseAdapter;

	@Override
	public AuthenticationWebResponse<SignupBody> signup(final SignupRequest request)
	{
		return signupResponseAdapter.adapt(authenticationService.signup(signupRequestAdapter.adapt(request)));
	}

	@Override
	public AuthenticationWebResponse<SigninBody> signin(final SigninRequest request)
	{
		return signinResponseAdapter.adapt(authenticationService.signin(signinRequestAdapter.adapt(request)));
	}

	protected AbstractAuthenticationWebFacade(
			final AuthenticationService authenticationService,
			final SignupRequestToCommandAdapter<SignupRequest> signupRequestAdapter,
			final SigninRequestToCommandAdapter<SigninRequest> signinRequestAdapter,
			final SignupResultToWebResponseAdapter<SignupBody> signupResponseAdapter,
			final SigninResultToWebResponseAdapter<SigninBody> signinResponseAdapter)
	{
		this.authenticationService = Objects.requireNonNull(authenticationService,
				"authenticationService must not be null");
		this.signupRequestAdapter = Objects.requireNonNull(signupRequestAdapter,
				"signupRequestAdapter must not be null");
		this.signinRequestAdapter = Objects.requireNonNull(signinRequestAdapter,
				"signinRequestAdapter must not be null");
		this.signupResponseAdapter = Objects.requireNonNull(signupResponseAdapter,
				"signupResponseAdapter must not be null");
		this.signinResponseAdapter = Objects.requireNonNull(signinResponseAdapter,
				"signinResponseAdapter must not be null");
	}
}