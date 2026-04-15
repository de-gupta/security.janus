package de.gupta.security.janus.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationPolicy.defaults")
final class AuthenticationPolicyDefaultsTest
{
	@Test
	@DisplayName("should disable unlinked local signin")
	void shouldDisableUnlinkedLocalSignin()
	{
		assertThat(AuthenticationPolicy.defaults().allowUnlinkedLocalSignin())
				.as("default policy should be conservative")
				.isFalse();
	}
}