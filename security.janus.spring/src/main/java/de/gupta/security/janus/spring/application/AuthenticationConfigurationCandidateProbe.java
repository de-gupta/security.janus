package de.gupta.security.janus.spring.application;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.spring.configuration.JanusSupportedIdentityProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class AuthenticationConfigurationCandidateProbe
{
	private static final ResolvableType SIGNUP_PROVIDER_FUNCTION = ResolvableType.forClassWithGenerics(Function.class,
			SignupProviderCommand.class,
			ProviderSignupResult.class);
	private static final ResolvableType SIGNIN_PROVIDER_FUNCTION = ResolvableType.forClassWithGenerics(Function.class,
			SigninProviderCommand.class,
			ProviderSigninResult.class);
	private static final ResolvableType OPTIONAL_LOCAL_ACCOUNT_IDENTITY_VIEW =
			ResolvableType.forClassWithGenerics(Optional.class, LocalAccountIdentityView.class);
	private static final ResolvableType LOOKUP_BI_FUNCTION = ResolvableType.forClassWithGenerics(BiFunction.class,
			ResolvableType.forClass(String.class),
			ResolvableType.forClass(String.class),
			OPTIONAL_LOCAL_ACCOUNT_IDENTITY_VIEW);
	private static final ResolvableType LOOKUP_QUERY_FUNCTION = ResolvableType.forClassWithGenerics(Function.class,
			ResolvableType.forClass(LocalAccountLookupQuery.class),
			OPTIONAL_LOCAL_ACCOUNT_IDENTITY_VIEW);
	private static final ResolvableType CREATION_FUNCTION = ResolvableType.forClassWithGenerics(Function.class,
			LocalAccountCreationCommand.class,
			LocalAccountCreationResult.class);

	public static boolean hasRequiredCandidates(final ListableBeanFactory beanFactory)
	{
		final SpringBeanLookup lookup = new SpringBeanLookup(beanFactory);

		return hasIdentityProviderCandidate(lookup) &&
				hasLookupCandidate(lookup) &&
				hasCreationCandidate(lookup);
	}

	private static boolean hasIdentityProviderCandidate(final SpringBeanLookup lookup)
	{
		return lookup.hasAnyBean(IdentityProviderPort.class) ||
				lookup.hasAnyBean(SIGNUP_PROVIDER_FUNCTION) ||
				lookup.hasAnyBean(SIGNIN_PROVIDER_FUNCTION) ||
				lookup.hasAnyBean(JanusSupportedIdentityProvider.class);
	}

	private static boolean hasLookupCandidate(final SpringBeanLookup lookup)
	{
		return lookup.hasAnyBean(LocalAccountLookupPort.class) ||
				lookup.hasAnyBean(LOOKUP_BI_FUNCTION) ||
				lookup.hasAnyBean(LOOKUP_QUERY_FUNCTION);
	}

	private static boolean hasCreationCandidate(final SpringBeanLookup lookup)
	{
		return lookup.hasAnyBean(LocalAccountCreationPort.class) || lookup.hasAnyBean(CREATION_FUNCTION);
	}

	private AuthenticationConfigurationCandidateProbe()
	{
	}
}
