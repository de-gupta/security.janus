package de.gupta.security.core.api;

import de.gupta.security.core.support.TestAuthenticationContext;
import de.gupta.security.janus.core.api.AuthenticationConfiguration;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationConfiguration convenience overloads")
class AuthenticationConfigurationConvenienceOverloadTest
{
	@Test
	@DisplayName("should default policy and clock when only required ports are supplied")
	void shouldDefaultPolicyAndClockWhenOnlyRequiredPortsAreSupplied()
	{
		final TestAuthenticationContext context = new TestAuthenticationContext();

		final AuthenticationConfiguration configuration =
				AuthenticationConfiguration.of(context.identityProviderPort,
						context.localAccountLookupPort,
						context.localAccountCreationPort);

		assertThat(configuration.localAccountDuplicateCheckPort()).isEmpty();
		assertThat(configuration.authenticationPolicy()).isEqualTo(AuthenticationPolicy.defaults());
		assertThat(configuration.clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("should retain custom policy and default the clock when duplicate check is omitted")
	void shouldRetainCustomPolicyAndDefaultTheClockWhenDuplicateCheckIsOmitted()
	{
		final TestAuthenticationContext context = new TestAuthenticationContext();

		final AuthenticationConfiguration configuration =
				AuthenticationConfiguration.of(context.identityProviderPort,
						context.localAccountLookupPort,
						context.localAccountCreationPort,
						AuthenticationPolicy.allowingUnlinkedLocalSignin());

		assertThat(configuration.localAccountDuplicateCheckPort()).isEmpty();
		assertThat(configuration.authenticationPolicy()).isEqualTo(
				AuthenticationPolicy.allowingUnlinkedLocalSignin());
		assertThat(configuration.clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}

	@Test
	@DisplayName("should retain duplicate check and policy while defaulting the clock")
	void shouldRetainDuplicateCheckAndPolicyWhileDefaultingTheClock()
	{
		final TestAuthenticationContext context = new TestAuthenticationContext();

		final AuthenticationConfiguration configuration =
				AuthenticationConfiguration.of(context.identityProviderPort,
						context.localAccountLookupPort,
						context.localAccountCreationPort,
						context.localAccountDuplicateCheckPort,
						AuthenticationPolicy.allowingUnlinkedLocalSignin());

		assertThat(configuration.localAccountDuplicateCheckPort()).containsSame(context.localAccountDuplicateCheckPort);
		assertThat(configuration.authenticationPolicy()).isEqualTo(
				AuthenticationPolicy.allowingUnlinkedLocalSignin());
		assertThat(configuration.clock().getZone()).isEqualTo(ZoneOffset.UTC);
	}
}
