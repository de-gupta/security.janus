package de.gupta.security.janus.core.domain.model.provider;

public sealed interface ProviderSignupResult permits ProviderSignupSuccess, ProviderSignupFailure
{
}