package de.gupta.security.janus.spring.application;

import de.gupta.security.janus.core.adapter.local.*;
import de.gupta.security.janus.core.adapter.provider.IdentityProviderPort;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.AuthenticationConfiguration;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSigninResult;
import de.gupta.security.janus.core.domain.model.provider.ProviderSignupResult;
import de.gupta.security.janus.spring.adapter.FunctionalBeanAdapters;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class AuthenticationConfigurationAssembler
{
	private static final String IDENTITY_PROVIDER_ROLE = "IdentityProviderPort";
	private static final String LOOKUP_ROLE = "LocalAccountLookupPort";
	private static final String CREATION_ROLE = "LocalAccountCreationPort";
	private static final String DUPLICATE_CHECK_ROLE = "LocalAccountDuplicateCheckPort";
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
	private static final ResolvableType DUPLICATE_CHECK_PREDICATE = ResolvableType.forClassWithGenerics(Predicate.class,
			String.class);

	private final SpringBeanLookup beanLookup;

	public static AuthenticationConfigurationAssembler create(final ListableBeanFactory beanFactory)
	{
		return new AuthenticationConfigurationAssembler(new SpringBeanLookup(beanFactory));
	}

	public AuthenticationConfiguration assembleOrThrow()
	{
		final ResolutionResult<AuthenticationConfiguration> result = assemble();
		if (result instanceof Resolved<AuthenticationConfiguration>(AuthenticationConfiguration value))
		{
			return value;
		}

		throw new IllegalStateException(((Unresolved<AuthenticationConfiguration>) result).failure().message());
	}

	public ResolutionResult<AuthenticationConfiguration> assemble()
	{
		final ResolutionResult<IdentityProviderPort> identityProviderPort = resolveIdentityProviderPort();
		if (identityProviderPort instanceof Unresolved<IdentityProviderPort> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		final ResolutionResult<LocalAccountLookupPort> localAccountLookupPort = resolveLocalAccountLookupPort();
		if (localAccountLookupPort instanceof Unresolved<LocalAccountLookupPort> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		final ResolutionResult<LocalAccountCreationPort> localAccountCreationPort = resolveLocalAccountCreationPort();
		if (localAccountCreationPort instanceof Unresolved<LocalAccountCreationPort> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		final ResolutionResult<Optional<LocalAccountDuplicateCheckPort>> duplicateCheckPort =
				resolveLocalAccountDuplicateCheckPort();
		if (duplicateCheckPort instanceof Unresolved<Optional<LocalAccountDuplicateCheckPort>> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		final ResolutionResult<AuthenticationPolicy> authenticationPolicy = resolveAuthenticationPolicy();
		if (authenticationPolicy instanceof Unresolved<AuthenticationPolicy> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		final ResolutionResult<Clock> clock = resolveClock();
		if (clock instanceof Unresolved<Clock> unresolved)
		{
			return unresolvedConfiguration(unresolved);
		}

		return ResolutionResult.resolved(new AuthenticationConfiguration(
				((Resolved<IdentityProviderPort>) identityProviderPort).value(),
				((Resolved<LocalAccountLookupPort>) localAccountLookupPort).value(),
				((Resolved<LocalAccountCreationPort>) localAccountCreationPort).value(),
				((Resolved<Optional<LocalAccountDuplicateCheckPort>>) duplicateCheckPort).value(),
				((Resolved<AuthenticationPolicy>) authenticationPolicy).value(),
				((Resolved<Clock>) clock).value()));
	}

	private ResolutionResult<IdentityProviderPort> resolveIdentityProviderPort()
	{
		final ResolutionResult<IdentityProviderPort> explicitPort = resolveExplicitPort(IdentityProviderPort.class,
				IDENTITY_PROVIDER_ROLE);
		if (explicitPort instanceof Resolved<IdentityProviderPort>)
		{
			return explicitPort;
		}
		if (isAmbiguous(explicitPort))
		{
			return explicitPort;
		}

		final ResolutionResult<Function<SignupProviderCommand, ProviderSignupResult>> signupFunction =
				resolveSingleFallback(SIGNUP_PROVIDER_FUNCTION,
						"Function<SignupProviderCommand, ProviderSignupResult>",
						IDENTITY_PROVIDER_ROLE);
		if (isAmbiguous(signupFunction))
		{
			return unresolvedIdentityProvider(signupFunction);
		}

		final ResolutionResult<Function<SigninProviderCommand, ProviderSigninResult>> signinFunction =
				resolveSingleFallback(SIGNIN_PROVIDER_FUNCTION,
						"Function<SigninProviderCommand, ProviderSigninResult>",
						IDENTITY_PROVIDER_ROLE);
		if (isAmbiguous(signinFunction))
		{
			return unresolvedIdentityProvider(signinFunction);
		}

		final boolean hasSignup =
				signupFunction instanceof Resolved<Function<SignupProviderCommand, ProviderSignupResult>>;
		final boolean hasSignin =
				signinFunction instanceof Resolved<Function<SigninProviderCommand, ProviderSigninResult>>;
		if (!hasSignup && !hasSignin)
		{
			return ResolutionResult.missing(missingRequiredCollaboratorMessage(IDENTITY_PROVIDER_ROLE,
					"exactly one IdentityProviderPort bean or both Function<SignupProviderCommand, ProviderSignupResult> and Function<SigninProviderCommand, ProviderSigninResult> beans"));
		}
		if (!hasSignup || !hasSignin)
		{
			return ResolutionResult.incomplete("Unable to resolve required collaborator '" + IDENTITY_PROVIDER_ROLE +
					"'. When using function-based fallback you must provide both Function<SignupProviderCommand, ProviderSignupResult> and Function<SigninProviderCommand, ProviderSigninResult> beans.");
		}

		return ResolutionResult.resolved(FunctionalBeanAdapters.identityProvider(
				((Resolved<Function<SignupProviderCommand, ProviderSignupResult>>) signupFunction).value(),
				((Resolved<Function<SigninProviderCommand, ProviderSigninResult>>) signinFunction).value()));
	}

	private ResolutionResult<LocalAccountLookupPort> resolveLocalAccountLookupPort()
	{
		final ResolutionResult<LocalAccountLookupPort> explicitPort = resolveExplicitPort(LocalAccountLookupPort.class,
				LOOKUP_ROLE);
		if (explicitPort instanceof Resolved<LocalAccountLookupPort>)
		{
			return explicitPort;
		}
		if (isAmbiguous(explicitPort))
		{
			return explicitPort;
		}

		final ResolutionResult<BiFunction<String, String, Optional<LocalAccountIdentityView>>> simpleLookup =
				resolveSingleFallback(LOOKUP_BI_FUNCTION,
						"BiFunction<String, String, Optional<LocalAccountIdentityView>>",
						LOOKUP_ROLE);
		if (isAmbiguous(simpleLookup))
		{
			return unresolvedLookup(simpleLookup);
		}

		final ResolutionResult<Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>> queryLookup =
				resolveSingleFallback(LOOKUP_QUERY_FUNCTION,
						"Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>",
						LOOKUP_ROLE);
		if (isAmbiguous(queryLookup))
		{
			return unresolvedLookup(queryLookup);
		}

		final boolean hasSimpleLookup =
				simpleLookup instanceof Resolved<BiFunction<String, String, Optional<LocalAccountIdentityView>>>;
		final boolean hasQueryLookup =
				queryLookup instanceof Resolved<Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>>;
		if (hasSimpleLookup && hasQueryLookup)
		{
			return ResolutionResult.ambiguous("Unable to resolve required collaborator '" + LOOKUP_ROLE +
					"'. Both lookup fallback shapes are present. Provide exactly one of BiFunction<String, String, Optional<LocalAccountIdentityView>> or Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>, or provide an explicit LocalAccountLookupPort bean.");
		}
		if (hasSimpleLookup)
		{
			return ResolutionResult.resolved(FunctionalBeanAdapters.localAccountLookup(
					((Resolved<BiFunction<String, String, Optional<LocalAccountIdentityView>>>) simpleLookup).value()));
		}
		if (hasQueryLookup)
		{
			return ResolutionResult.resolved(FunctionalBeanAdapters.localAccountLookup(
					((Resolved<Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>>) queryLookup).value()));
		}

		return ResolutionResult.missing(missingRequiredCollaboratorMessage(LOOKUP_ROLE,
				"exactly one LocalAccountLookupPort bean, one BiFunction<String, String, Optional<LocalAccountIdentityView>> bean, or one Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>> bean"));
	}

	private ResolutionResult<LocalAccountCreationPort> resolveLocalAccountCreationPort()
	{
		final ResolutionResult<LocalAccountCreationPort> explicitPort =
				resolveExplicitPort(LocalAccountCreationPort.class,
						CREATION_ROLE);
		if (explicitPort instanceof Resolved<LocalAccountCreationPort>)
		{
			return explicitPort;
		}
		if (isAmbiguous(explicitPort))
		{
			return explicitPort;
		}

		final ResolutionResult<Function<LocalAccountCreationCommand, LocalAccountCreationResult>> creationFunction =
				resolveSingleFallback(CREATION_FUNCTION,
						"Function<LocalAccountCreationCommand, LocalAccountCreationResult>",
						CREATION_ROLE);
		if (creationFunction instanceof Resolved<Function<LocalAccountCreationCommand, LocalAccountCreationResult>>(
				Function<LocalAccountCreationCommand, LocalAccountCreationResult> value
		))
		{
			return ResolutionResult.resolved(FunctionalBeanAdapters.localAccountCreation(value));
		}
		if (isAmbiguous(creationFunction))
		{
			return unresolvedCreation(creationFunction);
		}

		return ResolutionResult.missing(missingRequiredCollaboratorMessage(CREATION_ROLE,
				"exactly one LocalAccountCreationPort bean or one Function<LocalAccountCreationCommand, LocalAccountCreationResult> bean"));
	}

	private ResolutionResult<Optional<LocalAccountDuplicateCheckPort>> resolveLocalAccountDuplicateCheckPort()
	{
		final ResolutionResult<LocalAccountDuplicateCheckPort> explicitPort =
				resolveExplicitPort(LocalAccountDuplicateCheckPort.class, DUPLICATE_CHECK_ROLE);
		if (explicitPort instanceof Resolved<LocalAccountDuplicateCheckPort>(LocalAccountDuplicateCheckPort value))
		{
			return ResolutionResult.resolved(Optional.of(value));
		}
		if (isAmbiguous(explicitPort))
		{
			return unresolvedOptional(explicitPort);
		}

		final ResolutionResult<Predicate<String>> duplicateCheckPredicate =
				resolveSingleFallback(DUPLICATE_CHECK_PREDICATE,
						"Predicate<String>",
						DUPLICATE_CHECK_ROLE);
		if (duplicateCheckPredicate instanceof Resolved<Predicate<String>>(Predicate<String> value))
		{
			return ResolutionResult.resolved(Optional.of(FunctionalBeanAdapters.localAccountDuplicateCheck(
					value)));
		}
		if (isAmbiguous(duplicateCheckPredicate))
		{
			return unresolvedOptional(duplicateCheckPredicate);
		}

		return ResolutionResult.resolved(Optional.empty());
	}

	private ResolutionResult<AuthenticationPolicy> resolveAuthenticationPolicy()
	{
		final List<String> beanNames = beanLookup.beanNames(AuthenticationPolicy.class);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.resolved(AuthenticationPolicy.defaults());
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous(ambiguousMessage("AuthenticationPolicy", beanNames,
					"Provide exactly one AuthenticationPolicy bean or let Janus Spring use AuthenticationPolicy.defaults()."));
		}

		return ResolutionResult.resolved(beanLookup.bean(beanNames.getFirst(), AuthenticationPolicy.class));
	}

	private ResolutionResult<Clock> resolveClock()
	{
		final List<String> beanNames = beanLookup.beanNames(Clock.class);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.resolved(Clock.systemUTC());
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous(ambiguousMessage("Clock", beanNames,
					"Provide exactly one Clock bean or let Janus Spring use Clock.systemUTC()."));
		}

		return ResolutionResult.resolved(beanLookup.bean(beanNames.getFirst(), Clock.class));
	}

	private <T> ResolutionResult<T> resolveExplicitPort(final Class<T> type, final String role)
	{
		final List<String> beanNames = beanLookup.beanNames(type);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.missing("No explicit " + role + " bean found.");
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous(ambiguousMessage(role, beanNames,
					"Janus Spring prefers an explicit " + role + " bean over function-based fallbacks, so exactly one bean is required."));
		}

		return ResolutionResult.resolved(beanLookup.bean(beanNames.getFirst(), type));
	}

	@SuppressWarnings("unchecked")
	private <T> ResolutionResult<T> resolveSingleFallback(final ResolvableType type,
	                                                      final String beanShape,
	                                                      final String role)
	{
		final List<String> beanNames = beanLookup.beanNames(type);
		if (beanNames.isEmpty())
		{
			return ResolutionResult.missing("No fallback " + beanShape + " bean found for " + role + ".");
		}
		if (beanNames.size() > 1)
		{
			return ResolutionResult.ambiguous(ambiguousMessage(beanShape, beanNames,
					"Provide exactly one " + beanShape + " bean or an explicit " + role + " bean."));
		}

		return ResolutionResult.resolved((T) beanLookup.bean(beanNames.getFirst()));
	}

	private boolean isAmbiguous(final ResolutionResult<?> result)
	{
		return result instanceof Unresolved<?>(ResolutionFailure failure) &&
				failure.kind() == ResolutionFailureKind.AMBIGUOUS;
	}

	private ResolutionResult<IdentityProviderPort> unresolvedIdentityProvider(final ResolutionResult<?> result)
	{
		return ResolutionResult.ambiguous(((Unresolved<?>) result).failure().message());
	}

	private ResolutionResult<LocalAccountLookupPort> unresolvedLookup(final ResolutionResult<?> result)
	{
		return ResolutionResult.ambiguous(((Unresolved<?>) result).failure().message());
	}

	private ResolutionResult<LocalAccountCreationPort> unresolvedCreation(final ResolutionResult<?> result)
	{
		return ResolutionResult.ambiguous(((Unresolved<?>) result).failure().message());
	}

	private ResolutionResult<Optional<LocalAccountDuplicateCheckPort>> unresolvedOptional(
			final ResolutionResult<?> result)
	{
		return ResolutionResult.ambiguous(((Unresolved<?>) result).failure().message());
	}

	private ResolutionResult<AuthenticationConfiguration> unresolvedConfiguration(final Unresolved<?> unresolved)
	{
		return new Unresolved<>(unresolved.failure());
	}

	private String missingRequiredCollaboratorMessage(final String role, final String acceptedShapes)
	{
		return "Unable to resolve required collaborator '" + role + "'. Provide " + acceptedShapes + ".";
	}

	private String ambiguousMessage(final String role, final List<String> beanNames, final String guidance)
	{
		return "Multiple beans found for '" + role + "': " + beanNames + ". " + guidance;
	}

	private AuthenticationConfigurationAssembler(final SpringBeanLookup beanLookup)
	{
		this.beanLookup = beanLookup;
	}
}