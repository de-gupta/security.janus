package de.gupta.security.janus.adapter.local;

import de.gupta.security.janus.api.command.SignupProfileAttributes;
import de.gupta.security.janus.support.TestFixtures;
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

@DisplayName("LocalAccountCreationCommand.of")
class LocalAccountCreationCommandOfTest
{
	private static Stream<Arguments> rawInputCases()
	{
		return Stream.of(
							 new RawInputCase("null login identifier",
									 () -> LocalAccountCreationCommand.of(null, SignupProfileAttributes.empty(), "local",
											 TestFixtures.providerIdentity(), Optional.empty()),
									 new Object[]{null, SignupProfileAttributes.empty(), "local", TestFixtures.providerIdentity(),
											 Optional.empty()}),
							 new RawInputCase("null profile attributes",
									 () -> LocalAccountCreationCommand.of("ada@example.com", null, "local",
											 TestFixtures.providerIdentity(), Optional.empty()),
									 new Object[]{"ada@example.com", null, "local", TestFixtures.providerIdentity(),
											 Optional.empty()}),
							 new RawInputCase("null provider session optional",
									 () -> LocalAccountCreationCommand.of("ada@example.com", SignupProfileAttributes.empty(),
											 "local", TestFixtures.providerIdentity(), null),
									 new Object[]{"ada@example.com", SignupProfileAttributes.empty(), "local",
											 TestFixtures.providerIdentity(), null}))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
							 testCase.expectedValues()));
	}

	private record RawInputCase(String description, Supplier<LocalAccountCreationCommand> invocation,
	                            Object[] expectedValues)
	{
	}

	@Nested
	@DisplayName("carrier behavior")
	class CarrierBehavior
	{
		@Test
		@DisplayName("should create a local account creation command")
		void shouldCreateALocalAccountCreationCommand()
		{
			assertThat(LocalAccountCreationCommand.of("ada@example.com",
					TestFixtures.signupProfileAttributes(),
					"local",
					TestFixtures.providerIdentity(),
					Optional.of(TestFixtures.providerSession())))
					.as("created local account creation command")
					.extracting(LocalAccountCreationCommand::loginIdentifier,
							LocalAccountCreationCommand::profileAttributes,
							LocalAccountCreationCommand::requestedProvider,
							LocalAccountCreationCommand::providerIdentity,
							LocalAccountCreationCommand::providerSession)
					.containsExactly("ada@example.com",
							TestFixtures.signupProfileAttributes(),
							"local",
							TestFixtures.providerIdentity(),
							Optional.of(TestFixtures.providerSession()));
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.adapter.local.LocalAccountCreationCommandOfTest#rawInputCases")
		@DisplayName("should retain raw malformed values for orchestrator-controlled validation")
		void shouldRetainRawMalformedValuesForOrchestratorControlledValidation(final String description,
		                                                                       final Supplier<LocalAccountCreationCommand> invocation,
		                                                                       final Object[] expectedValues)
		{
			assertThat(invocation.get())
					.as(description)
					.extracting(LocalAccountCreationCommand::loginIdentifier,
							LocalAccountCreationCommand::profileAttributes,
							LocalAccountCreationCommand::requestedProvider,
							LocalAccountCreationCommand::providerIdentity,
							LocalAccountCreationCommand::providerSession)
					.containsExactly(expectedValues);
		}
	}
}