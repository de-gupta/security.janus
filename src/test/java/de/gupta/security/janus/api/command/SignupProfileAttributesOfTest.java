package de.gupta.security.janus.api.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignupProfileAttributes.of")
class SignupProfileAttributesOfTest
{
	private static Stream<Arguments> rawInputCases()
	{
		return Stream.of(
							 new RawInputCase("null email optional",
									 () -> SignupProfileAttributes.of(null, Optional.empty(), Optional.empty(), Optional.empty()),
									 new Object[]{null, Optional.empty(), Optional.empty(), Optional.empty()}),
							 new RawInputCase("blank first name value",
									 () -> SignupProfileAttributes.of(Optional.empty(), Optional.of(" "), Optional.empty(),
											 Optional.empty()),
									 new Object[]{Optional.empty(), Optional.of(" "), Optional.empty(), Optional.empty()}),
							 new RawInputCase("blank display name value",
									 () -> SignupProfileAttributes.of(Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.of(" ")),
									 new Object[]{Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(" ")}))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
							 testCase.expectedValues()));
	}

	private record RawInputCase(String description, Supplier<SignupProfileAttributes> invocation,
	                            Object[] expectedValues)
	{
	}

	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should preserve provided values")
		void shouldPreserveProvidedValues()
		{
			assertThat(SignupProfileAttributes.of(Optional.of(" ada@example.com "),
					Optional.of(" Ada "),
					Optional.of(" Lovelace "),
					Optional.of(" Enchantress ")))
					.as("profile attributes are request carriers")
					.extracting(SignupProfileAttributes::email,
							SignupProfileAttributes::firstName,
							SignupProfileAttributes::lastName,
							SignupProfileAttributes::displayName)
					.containsExactly(Optional.of(" ada@example.com "),
							Optional.of(" Ada "),
							Optional.of(" Lovelace "),
							Optional.of(" Enchantress "));
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.api.command.SignupProfileAttributesOfTest#rawInputCases")
		@DisplayName("should retain malformed optionals for later validation")
		void shouldRetainMalformedOptionalsForLaterValidation(final String description,
		                                                      final Supplier<SignupProfileAttributes> invocation,
		                                                      final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(SignupProfileAttributes::email,
							SignupProfileAttributes::firstName,
							SignupProfileAttributes::lastName,
							SignupProfileAttributes::displayName)
					.containsExactly(expectedValues);
		}
	}
}