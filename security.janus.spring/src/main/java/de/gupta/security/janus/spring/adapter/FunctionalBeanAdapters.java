package de.gupta.security.janus.spring.adapter;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FunctionalBeanAdapters
{
	public static IdentityProviderPort identityProvider(
			final Function<SignupProviderCommand, ProviderSignupResult> signupFunction,
			final Function<SigninProviderCommand, ProviderSigninResult> signinFunction)
	{
		return IdentityProviderPort.of(signupFunction, signinFunction);
	}

	public static LocalAccountLookupPort localAccountLookup(
			final BiFunction<String, String, Optional<LocalAccountIdentityView>> lookupFunction)
	{
		return LocalAccountLookupPort.of(lookupFunction);
	}

	public static LocalAccountLookupPort localAccountLookup(
			final Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>> lookupFunction)
	{
		return LocalAccountLookupPort.of(lookupFunction);
	}

	public static LocalAccountCreationPort localAccountCreation(
			final Function<LocalAccountCreationCommand, LocalAccountCreationResult> creationFunction)
	{
		return LocalAccountCreationPort.of(creationFunction);
	}

	public static LocalAccountDuplicateCheckPort localAccountDuplicateCheck(
			final Predicate<String> duplicateCheckPredicate)
	{
		return LocalAccountDuplicateCheckPort.of(duplicateCheckPredicate);
	}

	private FunctionalBeanAdapters()
	{
	}
}