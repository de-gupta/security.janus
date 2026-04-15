package de.gupta.security.janus.domain.model.provider;

public sealed interface ProviderSigninResult permits ProviderSigninSuccess, ProviderSigninFailure
{
}
