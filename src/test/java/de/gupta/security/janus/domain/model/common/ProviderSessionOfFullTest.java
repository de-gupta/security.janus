package de.gupta.security.janus.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ProviderSession.of(provider, sessionId, accessToken, refreshToken, idToken, issuedAt, expiresAt, metadata)")
class ProviderSessionOfFullTest
{
	private static Stream<Arguments> invalidCases()
	{
		return Stream.of(
							 new InvalidCase("null provider",
									 () -> ProviderSession.of(null, Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.empty(), Optional.empty(), Optional.empty(), Map.of()),
									 IllegalArgumentException.class,
									 "provider must not be blank"),
							 new InvalidCase("null session id optional",
									 () -> ProviderSession.of("local", null, Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.empty(), Optional.empty(), Map.of()),
									 NullPointerException.class,
									 null),
							 new InvalidCase("blank access token",
									 () -> ProviderSession.of("local", Optional.empty(), Optional.of(" "), Optional.empty(),
											 Optional.empty(), Optional.empty(), Optional.empty(), Map.of()),
									 IllegalArgumentException.class,
									 "optional string value must not be blank"),
							 new InvalidCase("null issued at optional",
									 () -> ProviderSession.of("local", Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.empty(), null, Optional.empty(), Map.of()),
									 NullPointerException.class,
									 "issuedAt must not be null"),
							 new InvalidCase("null metadata",
									 () -> ProviderSession.of("local", Optional.empty(), Optional.empty(), Optional.empty(),
											 Optional.empty(), Optional.empty(), Optional.empty(), null),
									 NullPointerException.class,
									 "metadata must not be null"))
		             .map(testCase -> Arguments.of(testCase.description(), testCase.invocation(),
							 testCase.expectedType(), testCase.expectedMessage()));
	}

	private record InvalidCase(String description,
	                           Supplier<ProviderSession> invocation,
	                           Class<? extends Throwable> expectedType,
	                           String expectedMessage)
	{
	}

	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should trim token values and defensively copy metadata")
		void shouldTrimTokenValuesAndDefensivelyCopyMetadata()
		{
			final Map<String, Object> mutableMetadata = new HashMap<>();
			mutableMetadata.put("scope", "basic");

			final ProviderSession session = ProviderSession.of("local",
					Optional.of(" session-1 "),
					Optional.of(" access-1 "),
					Optional.of(" refresh-1 "),
					Optional.of(" id-1 "),
					Optional.of(Instant.parse("2026-04-15T10:15:30Z")),
					Optional.of(Instant.parse("2026-04-15T11:15:30Z")),
					mutableMetadata);
			mutableMetadata.put("scope", "changed");

			assertThat(session)
					.as("sanitized provider session")
					.extracting(ProviderSession::provider,
							ProviderSession::sessionId,
							ProviderSession::accessToken,
							ProviderSession::refreshToken,
							ProviderSession::idToken)
					.containsExactly("local",
							Optional.of("session-1"),
							Optional.of("access-1"),
							Optional.of("refresh-1"),
							Optional.of("id-1"));
			assertThat(session.metadata()).as("metadata should be copied").containsExactly(entry("scope", "basic"));
			assertThatThrownBy(() -> session.metadata().put("new", "value"))
					.as("metadata should be immutable")
					.isInstanceOf(UnsupportedOperationException.class);
		}
	}

	@Nested
	@DisplayName("invalid arguments")
	class InvalidArguments
	{
		@ParameterizedTest(name = "{0}")
		@MethodSource("de.gupta.security.janus.domain.model.common.ProviderSessionOfFullTest#invalidCases")
		@DisplayName("should reject null optionals and blank present values")
		void shouldRejectNullOptionalsAndBlankPresentValues(final String description,
		                                                    final Supplier<ProviderSession> invocation,
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
	}
}