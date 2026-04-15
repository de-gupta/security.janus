package de.gupta.security.janus.core.domain.model.signin;

public sealed interface SigninResult permits SigninSuccess, SigninFailure
{
}