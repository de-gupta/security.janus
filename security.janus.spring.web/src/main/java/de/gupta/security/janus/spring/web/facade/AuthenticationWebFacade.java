package de.gupta.security.janus.spring.web.facade;

public interface AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody>
{
	AuthenticationWebResponse<SignupBody> signup(SignupRequest request);

	AuthenticationWebResponse<SigninBody> signin(SigninRequest request);
}