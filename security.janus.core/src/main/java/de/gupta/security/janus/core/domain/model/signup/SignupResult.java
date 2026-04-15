package de.gupta.security.janus.core.domain.model.signup;

public sealed interface SignupResult permits SignupSuccess, SignupFailure
{
}