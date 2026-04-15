package de.gupta.security.janus.application.service;

import de.gupta.commons.utility.string.StringSanitizationUtility;
import de.gupta.security.janus.adapter.local.LocalAccountCreationCommand;
import de.gupta.security.janus.adapter.local.LocalAccountIdentityView;
import de.gupta.security.janus.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.api.AuthenticationConfiguration;
import de.gupta.security.janus.api.command.SigninCommand;
import de.gupta.security.janus.api.command.SignupCommand;
import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.domain.model.common.LocalAccountReference;
import de.gupta.security.janus.domain.model.common.ProviderIdentity;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationFailure;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationResult;
import de.gupta.security.janus.domain.model.local.LocalAccountCreationSuccess;
import de.gupta.security.janus.domain.model.provider.*;
import de.gupta.security.janus.domain.model.signin.SigninFailure;
import de.gupta.security.janus.domain.model.signin.SigninFailureReason;
import de.gupta.security.janus.domain.model.signin.SigninResult;
import de.gupta.security.janus.domain.model.signin.SigninSuccess;
import de.gupta.security.janus.domain.model.signup.*;

import java.time.Instant;
import java.util.Optional;

final class InternalAuthenticationServiceImpl implements InternalAuthenticationService
{
	private final AuthenticationConfiguration configuration;

	@Override
	public SignupResult signup(final SignupCommand command, final Instant issuedAt)
	{
		if (invalidSignupCommand(command))
		{
			return SignupFailure.of(SignupFailureReason.INVALID_INPUT, "signup command is malformed");
		}

		if (isDuplicateLocalAccount(command.loginIdentifier()))
		{
			return SignupFailure.of(SignupFailureReason.DUPLICATE_LOCAL_ACCOUNT,
					"local account already exists for login identifier");
		}

		final ProviderSignupResult providerResult = configuration.identityProviderPort()
		                                                       .signup(SignupProviderCommand.of(
				                                                       command.loginIdentifier(),
				                                                       command.rawSecret(),
				                                                       command.profileAttributes(),
				                                                       command.requestedProvider()));
		if (providerResult instanceof ProviderSignupFailure providerFailure)
		{
			return mapProviderSignupFailure(providerFailure);
		}

		final ProviderSignupSuccess providerSuccess = (ProviderSignupSuccess) providerResult;
		final LocalAccountCreationResult localAccountCreationResult = configuration.localAccountCreationPort()
		        .createLocalAccount(LocalAccountCreationCommand.of(command.loginIdentifier(),
				        command.profileAttributes(),
				        command.requestedProvider(),
				        providerSuccess.providerIdentity(),
				        providerSuccess.providerSession()));

		if (localAccountCreationResult instanceof LocalAccountCreationFailure localFailure)
		{
			return mapLocalAccountCreationFailure(localFailure, providerSuccess.providerIdentity());
		}

		final LocalAccountCreationSuccess localSuccess = (LocalAccountCreationSuccess) localAccountCreationResult;
		return SignupSuccess.of(providerSuccess.providerIdentity(),
				localSuccess.localAccount(),
				providerSuccess.providerSession(),
				SignupCompletion.completed(providerSuccess.providerSession().isPresent()));
	}

	@Override
	public SigninResult signin(final SigninCommand command, final Instant issuedAt)
	{
		if (invalidSigninCommand(command))
		{
			return SigninFailure.of(SigninFailureReason.MALFORMED_COMMAND, "signin command is malformed");
		}

		final ProviderSigninResult providerResult = configuration.identityProviderPort()
		                                                       .signin(SigninProviderCommand.of(
				                                                       command.loginIdentifier(),
				                                                       command.rawSecret(),
				                                                       command.requestedProvider()));
		if (providerResult instanceof ProviderSigninFailure providerFailure)
		{
			return mapProviderSigninFailure(providerFailure);
		}

		final ProviderSigninSuccess providerSuccess = (ProviderSigninSuccess) providerResult;
		final Optional<LocalAccountIdentityView> localAccount = configuration.localAccountLookupPort()
		                                                             .findByProviderSubject(
				                                                             providerSuccess.providerIdentity().provider(),
				                                                             providerSuccess.providerIdentity()
				                                                                            .externalSubject());
		if (localAccount.isEmpty())
		{
			return configuration.authenticationPolicy().allowUnlinkedLocalSignin()
					? SigninSuccess.of(providerSuccess.providerSession(),
					providerSuccess.providerIdentity(),
					Optional.empty())
					: SigninFailure.of(SigninFailureReason.LOCAL_ACCOUNT_NOT_LINKED,
					"no linked local account found");
		}

		if (!localAccount.get().active())
		{
			return SigninFailure.of(SigninFailureReason.LOCAL_ACCOUNT_DISABLED, "linked local account is disabled");
		}

		return SigninSuccess.of(providerSuccess.providerSession(),
				providerSuccess.providerIdentity(),
				Optional.of(toLocalAccountReference(localAccount.get())));
	}

	private boolean isDuplicateLocalAccount(final String loginIdentifier)
	{
		return configuration.localAccountDuplicateCheckPort()
		                    .map(port -> port.existsByLoginIdentifier(loginIdentifier))
		                    .orElse(false);
	}

	private SignupFailure mapProviderSignupFailure(final ProviderSignupFailure failure)
	{
		return switch (failure.reason())
		{
			case DUPLICATE_IDENTITY -> buildSignupFailure(SignupFailureReason.DUPLICATE_PROVIDER_IDENTITY, failure.details());
			case PROVIDER_REJECTED -> buildSignupFailure(SignupFailureReason.PROVIDER_REJECTED, failure.details());
			case PROVIDER_UNAVAILABLE -> buildSignupFailure(SignupFailureReason.PROVIDER_UNAVAILABLE, failure.details());
			case UNSUPPORTED_PROVIDER -> buildSignupFailure(SignupFailureReason.UNSUPPORTED_PROVIDER, failure.details());
			case MALFORMED_COMMAND -> buildSignupFailure(SignupFailureReason.INVALID_INPUT, failure.details());
			case INTERNAL_ERROR -> buildSignupFailure(SignupFailureReason.UNEXPECTED_INTERNAL_FAILURE, failure.details());
		};
	}

	private SignupFailure mapLocalAccountCreationFailure(final LocalAccountCreationFailure failure,
	                                                     final ProviderIdentity providerIdentity)
	{
		final String details = failure.details().orElse("local account creation failed after provider provisioning");
		return SignupFailure.of(SignupFailureReason.RECONCILIATION_REQUIRED, details, providerIdentity);
	}

	private SigninFailure mapProviderSigninFailure(final ProviderSigninFailure failure)
	{
		return switch (failure.reason())
		{
			case INVALID_CREDENTIALS -> buildSigninFailure(SigninFailureReason.INVALID_CREDENTIALS, failure.details());
			case PROVIDER_UNAVAILABLE -> buildSigninFailure(SigninFailureReason.PROVIDER_UNAVAILABLE, failure.details());
			case PROVIDER_REJECTED -> buildSigninFailure(SigninFailureReason.PROVIDER_REJECTED, failure.details());
			case UNSUPPORTED_PROVIDER -> buildSigninFailure(SigninFailureReason.UNSUPPORTED_PROVIDER, failure.details());
			case MALFORMED_COMMAND -> buildSigninFailure(SigninFailureReason.MALFORMED_COMMAND, failure.details());
			case INTERNAL_ERROR -> buildSigninFailure(SigninFailureReason.UNEXPECTED_INTERNAL_FAILURE, failure.details());
		};
	}

	private SignupFailure buildSignupFailure(final SignupFailureReason reason, final Optional<String> details)
	{
		return details.map(detail -> SignupFailure.of(reason, detail))
		              .orElseGet(() -> SignupFailure.of(reason));
	}

	private SigninFailure buildSigninFailure(final SigninFailureReason reason, final Optional<String> details)
	{
		return details.map(detail -> SigninFailure.of(reason, detail))
		              .orElseGet(() -> SigninFailure.of(reason));
	}

	private LocalAccountReference toLocalAccountReference(final LocalAccountIdentityView localAccount)
	{
		return LocalAccountReference.of(localAccount.localAccountId(),
				localAccount.externalSubject(),
				localAccount.provider(),
				localAccount.active());
	}

	private boolean invalidSignupCommand(final SignupCommand command)
	{
		return command == null ||
				invalidRequiredString(command.loginIdentifier()) ||
				invalidRequiredString(command.rawSecret()) ||
				invalidRequiredString(command.requestedProvider()) ||
				invalidProfileAttributes(command.profileAttributes());
	}

	private boolean invalidSigninCommand(final SigninCommand command)
	{
		return command == null ||
				invalidRequiredString(command.loginIdentifier()) ||
				invalidRequiredString(command.rawSecret()) ||
				invalidRequiredString(command.requestedProvider());
	}

	private boolean invalidProfileAttributes(final SignupProfileAttributes profileAttributes)
	{
		return profileAttributes == null ||
				invalidOptionalString(profileAttributes.email()) ||
				invalidOptionalString(profileAttributes.firstName()) ||
				invalidOptionalString(profileAttributes.lastName()) ||
				invalidOptionalString(profileAttributes.displayName());
	}

	private boolean invalidRequiredString(final String value)
	{
		return StringSanitizationUtility.isAbsentOrBlank(value);
	}

	private boolean invalidOptionalString(final Optional<String> value)
	{
		return value == null || value.stream().anyMatch(StringSanitizationUtility::isAbsentOrBlank);
	}

	InternalAuthenticationServiceImpl(final AuthenticationConfiguration configuration)
	{
		this.configuration = configuration;
	}
}