package de.gupta.security.core.api;

import de.gupta.security.core.support.TestAuthenticationContext;
import de.gupta.security.janus.core.api.AuthenticationConfiguration;
import de.gupta.security.janus.core.api.AuthenticationPolicy;
import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.AuthenticationServiceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthenticationServiceFactory.create")
class AuthenticationServiceFactoryCreateTest
{
	@Test
	@DisplayName("should reject null configuration")
	void shouldRejectNullConfiguration()
	{
		assertThatThrownBy(() -> AuthenticationServiceFactory.create(null))
				.as("null configuration should fail fast")
				.isInstanceOf(NullPointerException.class)
				.hasMessage("configuration must not be null");
	}

	@Test
	@DisplayName("should create authentication service")
	void shouldCreateAuthenticationService()
	{
		final TestAuthenticationContext context = new TestAuthenticationContext();

		assertThat(AuthenticationServiceFactory.create(AuthenticationConfiguration.of(context.identityProviderPort,
				context.localAccountLookupPort,
				context.localAccountCreationPort,
				context.localAccountDuplicateCheckPort,
				AuthenticationPolicy.defaults(),
				context.clock)))
				.as("factory should create a public authentication service")
				.isNotNull()
				.isInstanceOf(AuthenticationService.class);
	}
}