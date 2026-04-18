package de.gupta.security.janus.spring.web.api;

import de.gupta.security.janus.spring.web.facade.AuthenticationWebFacade;
import de.gupta.security.janus.spring.web.facade.AuthenticationWebResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class AbstractSpringRestAuthenticationController<SignupRequest, SigninRequest, SignupBody, SigninBody>
		implements AuthenticationController<SignupRequest, SigninRequest, SignupBody, SigninBody>
{
	private final AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody> facade;

	@Override
	public ResponseEntity<SignupBody> signup(@RequestBody @Valid final SignupRequest request)
	{
		return toResponseEntity(facade.signup(request));
	}

	@Override
	public ResponseEntity<SigninBody> signin(@RequestBody @Valid final SigninRequest request)
	{
		return toResponseEntity(facade.signin(request));
	}

	private static <T> ResponseEntity<T> toResponseEntity(final AuthenticationWebResponse<T> response)
	{
		return ResponseEntity.status(response.status()).body(response.body());
	}

	protected AbstractSpringRestAuthenticationController(
			final AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody> facade)
	{
		this.facade = facade;
	}
}