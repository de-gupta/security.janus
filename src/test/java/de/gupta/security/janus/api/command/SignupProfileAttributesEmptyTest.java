package de.gupta.security.janus.api.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignupProfileAttributes.empty")
class SignupProfileAttributesEmptyTest
{
	@Test
	@DisplayName("should create an instance with all fields absent")
	void shouldCreateAnInstanceWithAllFieldsAbsent()
	{
		assertThat(SignupProfileAttributes.empty())
				.as("empty profile attributes")
				.extracting(SignupProfileAttributes::email,
						SignupProfileAttributes::firstName,
						SignupProfileAttributes::lastName,
						SignupProfileAttributes::displayName)
				.containsExactly(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
	}
}
