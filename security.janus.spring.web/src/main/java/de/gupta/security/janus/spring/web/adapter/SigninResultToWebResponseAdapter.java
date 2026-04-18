package de.gupta.security.janus.spring.web.adapter;

import de.gupta.security.janus.core.domain.model.signin.SigninResult;
import de.gupta.security.janus.spring.web.facade.AuthenticationWebResponse;

@FunctionalInterface
public interface SigninResultToWebResponseAdapter<SigninBody>
{
	AuthenticationWebResponse<SigninBody> adapt(SigninResult result);
}