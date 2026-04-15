package de.gupta.security.core.adapter.provider;

import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SigninProviderCommand.of")
class SigninProviderCommandOfTest
{
	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should create a provider signin command")
		void shouldCreateAProviderSigninCommand()
		{
			assertThat(SigninProviderCommand.of("ada@example.com", "secret", "local"))
					.as("created provider signin command")
					.extracting(SigninProviderCommand::loginIdentifier,
							SigninProviderCommand::rawSecret,
							SigninProviderCommand::requestedProvider)
					.containsExactly("ada@example.com", "secret", "local");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("rawInputCases")
		@DisplayName("should retain raw malformed values for provider-layer validation")
		void shouldRetainRawMalformedValuesForProviderLayerValidation(final String description,
		                                                              final Supplier<SigninProviderCommand> invocation,
		                                                              final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(SigninProviderCommand::loginIdentifier,
							SigninProviderCommand::rawSecret,
							SigninProviderCommand::requestedProvider)
					.containsExactly(expectedValues);
		}

		private static Stream<Arguments> rawInputCases()
		{
			return Stream.of(
								 new RawInputCase("null login identifier", () -> SigninProviderCommand.of(null, "secret", "local"),
										 new Object[]{null, "secret", "local"}),
								 new RawInputCase("blank raw secret",
										 () -> SigninProviderCommand.of("ada@example.com", " ", "local"),
										 new Object[]{"ada@example.com", " ", "local"}),
								 new RawInputCase("null provider", () -> SigninProviderCommand.of("ada@example.com", "secret", null),
										 new Object[]{"ada@example.com", "secret", null}))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedValues()));
		}

		private record RawInputCase(String description, Supplier<SigninProviderCommand> invocation,
		                            Object[] expectedValues)
		{
		}

	}
}