package de.gupta.security.janus.domain.model.provider;

public sealed interface ProviderSignupResult permits ProviderSignupSuccess, ProviderSignupFailure
{
}
