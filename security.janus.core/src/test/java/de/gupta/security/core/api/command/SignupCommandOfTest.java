package de.gupta.security.core.api.command;

import de.gupta.security.janus.core.api.command.SignupCommand;
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

@DisplayName("SignupCommand.of")
class SignupCommandOfTest
{
	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should create a signup command")
		void shouldCreateASignupCommand()
		{
			final SignupProfileAttributes attributes = SignupProfileAttributes.empty();

			assertThat(SignupCommand.of("ada@example.com", "secret", attributes, "local"))
					.as("created command")
					.extracting(SignupCommand::loginIdentifier,
							SignupCommand::rawSecret,
							SignupCommand::profileAttributes,
							SignupCommand::requestedProvider)
					.containsExactly("ada@example.com", "secret", attributes, "local");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("rawInputCases")
		@DisplayName("should retain raw malformed values for service-level validation")
		void shouldRetainRawMalformedValuesForServiceLevelValidation(final String description,
		                                                             final Supplier<SignupCommand> invocation,
		                                                             final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(SignupCommand::loginIdentifier,
							SignupCommand::rawSecret,
							SignupCommand::profileAttributes,
							SignupCommand::requestedProvider)
					.containsExactly(expectedValues);
		}

		private static Stream<Arguments> rawInputCases()
		{
			return Stream.of(
								 new RawInputCase("null login identifier",
										 () -> SignupCommand.of(null, "secret", SignupProfileAttributes.empty(), "local"),
										 new Object[]{null, "secret", SignupProfileAttributes.empty(), "local"}),
								 new RawInputCase("blank requested provider",
										 () -> SignupCommand.of("ada@example.com", "secret", SignupProfileAttributes.empty(), " "),
										 new Object[]{"ada@example.com", "secret", SignupProfileAttributes.empty(), " "}),
								 new RawInputCase("null profile attributes",
										 () -> SignupCommand.of("ada@example.com", "secret", null, "local"),
										 new Object[]{"ada@example.com", "secret", null, "local"}))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedValues()));
		}

		private record RawInputCase(String description, Supplier<SignupCommand> invocation, Object[] expectedValues)
		{
		}

	}
}