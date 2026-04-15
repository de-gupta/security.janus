package de.gupta.security.janus.domain.model.signup;

public sealed interface SignupResult permits SignupSuccess, SignupFailure
{
}
