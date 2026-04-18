package de.gupta.security.janus.spring.web.adapter;

import de.gupta.security.janus.core.domain.model.signup.SignupResult;
import de.gupta.security.janus.spring.web.facade.AuthenticationWebResponse;


@FunctionalInterface
public interface SignupResultToWebResponseAdapter<SignupBody>
{
	AuthenticationWebResponse<SignupBody> adapt(SignupResult result);
}