package de.gupta.security.janus.core.domain.model.provider;

public sealed interface ProviderSigninResult permits ProviderSigninSuccess, ProviderSigninFailure
{
}