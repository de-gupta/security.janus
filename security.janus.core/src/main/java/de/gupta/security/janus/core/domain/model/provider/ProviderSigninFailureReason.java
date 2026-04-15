package de.gupta.security.janus.core.domain.model.provider;

public enum ProviderSigninFailureReason
{
	INVALID_CREDENTIALS,
	PROVIDER_UNAVAILABLE,
	PROVIDER_REJECTED,
	UNSUPPORTED_PROVIDER,
	MALFORMED_COMMAND,
	INTERNAL_ERROR
}