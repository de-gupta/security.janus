package de.gupta.security.janus.domain.model.provider;

public enum ProviderSignupFailureReason
{
	DUPLICATE_IDENTITY,
	PROVIDER_REJECTED,
	PROVIDER_UNAVAILABLE,
	UNSUPPORTED_PROVIDER,
	MALFORMED_COMMAND,
	INTERNAL_ERROR
}
