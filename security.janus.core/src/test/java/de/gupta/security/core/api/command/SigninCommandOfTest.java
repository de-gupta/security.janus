package de.gupta.security.core.api.command;

import de.gupta.security.janus.core.api.command.SigninCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SigninCommand.of")
class SigninCommandOfTest
{
	private record RawInputCase(String description, Supplier<SigninCommand> invocation, Object[] expectedValues)
	{
	}

	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should create a signin command")
		void shouldCreateASigninCommand()
		{
			assertThat(SigninCommand.of("ada@example.com", "secret", "local"))
					.as("created command")
					.extracting(SigninCommand::loginIdentifier,
							SigninCommand::rawSecret,
							SigninCommand::requestedProvider)
					.containsExactly("ada@example.com", "secret", "local");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("rawInputCases")
		@DisplayName("should retain raw malformed values for service-level validation")
		void shouldRetainRawMalformedValuesForServiceLevelValidation(final String description,
		                                                             final Supplier<SigninCommand> invocation,
		                                                             final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(SigninCommand::loginIdentifier,
							SigninCommand::rawSecret,
							SigninCommand::requestedProvider)
					.containsExactly(expectedValues);
		}

		private static Stream<Arguments> rawInputCases()
		{
			return Stream.of(
								 new RawInputCase("null login identifier", () -> SigninCommand.of(null, "secret", "local"),
										 new Object[]{null, "secret", "local"}),
								 new RawInputCase("blank raw secret", () -> SigninCommand.of("ada@example.com", " ", "local"),
										 new Object[]{"ada@example.com", " ", "local"}),
								 new RawInputCase("null provider", () -> SigninCommand.of("ada@example.com", "secret", null),
										 new Object[]{"ada@example.com", "secret", null}))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedValues()));
		}

	}
}