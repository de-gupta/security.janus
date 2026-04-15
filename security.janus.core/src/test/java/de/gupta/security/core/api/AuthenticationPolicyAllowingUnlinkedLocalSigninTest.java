package de.gupta.security.core.api;

import de.gupta.security.janus.core.api.AuthenticationPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationPolicy.allowingUnlinkedLocalSignin")
class AuthenticationPolicyAllowingUnlinkedLocalSigninTest
{
	@Test
	@DisplayName("should enable unlinked local signin")
	void shouldEnableUnlinkedLocalSignin()
	{
		assertThat(AuthenticationPolicy.allowingUnlinkedLocalSignin().allowUnlinkedLocalSignin())
				.as("explicit permissive policy should allow unlinked signin")
				.isTrue();
	}
}