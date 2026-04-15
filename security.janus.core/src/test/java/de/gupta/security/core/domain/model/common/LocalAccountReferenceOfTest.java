package de.gupta.security.core.domain.model.common;

import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalAccountReference.of")
class LocalAccountReferenceOfTest
{
	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should create a local account reference")
		void shouldCreateALocalAccountReference()
		{
			assertThat(LocalAccountReference.of("local-account-1", Optional.of(" provider-subject "), "local", true))
					.as("created local account reference")
					.extracting(LocalAccountReference::localAccountId,
							LocalAccountReference::externalSubject,
							LocalAccountReference::provider,
							LocalAccountReference::active)
					.containsExactly("local-account-1", Optional.of(" provider-subject "), "local", true);
		}
	}

	@Nested
	@DisplayName("invalid arguments")
	class InvalidArguments
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("invalidCases")
		@DisplayName("should reject null and blank inputs")
		void shouldRejectNullAndBlankInputs(final String description,
		                                    final Supplier<LocalAccountReference> invocation,
		                                    final Class<? extends Throwable> expectedType,
		                                    final String expectedMessage)
		{
			assertThatThrownBy(invocation::get)
					.as(description)
					.isInstanceOf(expectedType)
					.satisfies(throwable ->
					{
						if (expectedMessage != null)
						{
							assertThat(throwable).as("exception message").hasMessage(expectedMessage);
						}
					});
		}

		private static Stream<Arguments> invalidCases()
		{
			return Stream.of(
								 new InvalidCase("null local account id",
										 () -> LocalAccountReference.of(null, Optional.empty(), "local", true),
										 IllegalArgumentException.class, "localAccountId must not be blank"),
								 new InvalidCase("blank local account id",
										 () -> LocalAccountReference.of(" ", Optional.empty(), "local", true),
										 IllegalArgumentException.class, "localAccountId must not be blank"),
								 new InvalidCase("null external subject optional",
										 () -> LocalAccountReference.of("local-account-1", null, "local", true),
										 NullPointerException.class, "externalSubject must not be null"),
								 new InvalidCase("blank external subject value",
										 () -> LocalAccountReference.of("local-account-1", Optional.of(" "), "local", true),
										 IllegalArgumentException.class, "externalSubject may not be blank"),
								 new InvalidCase("null provider",
										 () -> LocalAccountReference.of("local-account-1", Optional.empty(), null, true),
										 IllegalArgumentException.class, "provider must not be blank"))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedType(), testCase.expectedMessage()));
		}

		private record InvalidCase(String description,
		                           Supplier<LocalAccountReference> invocation,
		                           Class<? extends Throwable> expectedType,
		                           String expectedMessage)
		{
		}

	}
}
