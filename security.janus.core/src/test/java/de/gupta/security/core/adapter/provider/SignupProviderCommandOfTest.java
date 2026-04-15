package de.gupta.security.core.adapter.provider;

import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.command.SignupProfileAttributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignupProviderCommand.of")
class SignupProviderCommandOfTest
{
	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should create a provider signup command")
		void shouldCreateAProviderSignupCommand()
		{
			final SignupProfileAttributes attributes = SignupProfileAttributes.empty();

			assertThat(SignupProviderCommand.of("ada@example.com", "secret", attributes, "local"))
					.as("created provider signup command")
					.extracting(SignupProviderCommand::loginIdentifier,
							SignupProviderCommand::rawSecret,
							SignupProviderCommand::profileAttributes,
							SignupProviderCommand::requestedProvider)
					.containsExactly("ada@example.com", "secret", attributes, "local");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("rawInputCases")
		@DisplayName("should retain raw malformed values for provider-layer validation")
		void shouldRetainRawMalformedValuesForProviderLayerValidation(final String description,
		                                                              final Supplier<SignupProviderCommand> invocation,
		                                                              final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(SignupProviderCommand::loginIdentifier,
							SignupProviderCommand::rawSecret,
							SignupProviderCommand::profileAttributes,
							SignupProviderCommand::requestedProvider)
					.containsExactly(expectedValues);
		}

		private static Stream<Arguments> rawInputCases()
		{
			return Stream.of(
								 new RawInputCase("null login identifier",
										 () -> SignupProviderCommand.of(null, "secret", SignupProfileAttributes.empty(), "local"),
										 new Object[]{null, "secret", SignupProfileAttributes.empty(), "local"}),
								 new RawInputCase("null profile attributes",
										 () -> SignupProviderCommand.of("ada@example.com", "secret", null, "local"),
										 new Object[]{"ada@example.com", "secret", null, "local"}),
								 new RawInputCase("blank provider",
										 () -> SignupProviderCommand.of("ada@example.com", "secret", SignupProfileAttributes.empty(),
												 " "),
										 new Object[]{"ada@example.com", "secret", SignupProfileAttributes.empty(), " "}))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedValues()));
		}

		private record RawInputCase(String description, Supplier<SignupProviderCommand> invocation,
		                            Object[] expectedValues)
		{
		}

	}
}