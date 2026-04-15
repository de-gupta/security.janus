package de.gupta.security.janus.domain.model.signin;

public sealed interface SigninResult permits SigninSuccess, SigninFailure
{
}
