package de.gupta.security.core.domain.model.common;

import de.gupta.security.janus.core.domain.model.common.ProviderIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProviderIdentity.of(externalSubject, provider, metadata)")
class ProviderIdentityOfWithMetadataTest
{
	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should defensively copy metadata")
		void shouldDefensivelyCopyMetadata()
		{
			final Map<String, Object> mutableMetadata = new HashMap<>();
			mutableMetadata.put("tenant", "test");

			final ProviderIdentity identity = ProviderIdentity.of("provider-subject-1", "local", mutableMetadata);
			mutableMetadata.put("tenant", "changed");

			assertThat(identity.metadata()).as("stored metadata should be copied")
			                               .containsExactly(entry("tenant", "test"));
			assertThatThrownBy(() -> identity.metadata().put("new", "value"))
					.as("exposed metadata should be immutable")
					.isInstanceOf(UnsupportedOperationException.class);
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
		                                    final Supplier<ProviderIdentity> invocation,
		                                    final Class<? extends Throwable> expectedType,
		                                    final String expectedMessage)
		{
			assertThatThrownBy(invocation::get)
					.as(description)
					.isInstanceOf(expectedType)
					.hasMessage(expectedMessage);
		}

		private static Stream<Arguments> invalidCases()
		{
			return Stream.of(
								 new InvalidCase("null external subject", () -> ProviderIdentity.of(null, "local", Map.of()),
										 IllegalArgumentException.class, "externalSubject must not be blank"),
								 new InvalidCase("blank external subject", () -> ProviderIdentity.of(" ", "local", Map.of()),
										 IllegalArgumentException.class, "externalSubject must not be blank"),
								 new InvalidCase("null provider", () -> ProviderIdentity.of("provider-subject-1", null, Map.of()),
										 IllegalArgumentException.class, "provider must not be blank"),
								 new InvalidCase("null metadata", () -> ProviderIdentity.of("provider-subject-1", "local", null),
										 NullPointerException.class, "metadata must not be null"))
			             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
								 testCase.expectedType(), testCase.expectedMessage()));
		}

		private record InvalidCase(String description,
		                           Supplier<ProviderIdentity> invocation,
		                           Class<? extends Throwable> expectedType,
		                           String expectedMessage)
		{
		}

	}
}