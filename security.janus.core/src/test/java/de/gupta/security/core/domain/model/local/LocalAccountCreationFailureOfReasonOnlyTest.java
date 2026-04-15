package de.gupta.security.core.domain.model.local;

import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationFailure;
import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationFailureReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalAccountCreationFailure.of(reason)")
class LocalAccountCreationFailureOfReasonOnlyTest
{
	@Nested
	@DisplayName("success path")
	class SuccessPath
	{
		@Test
		@DisplayName("should create a failure without details")
		void shouldCreateAFailureWithoutDetails()
		{
			assertThat(LocalAccountCreationFailure.of(LocalAccountCreationFailureReason.REJECTED))
					.as("reason-only local account creation failure")
					.extracting(LocalAccountCreationFailure::reason, LocalAccountCreationFailure::details)
					.containsExactly(LocalAccountCreationFailureReason.REJECTED, Optional.empty());
		}
	}

	@Nested
	@DisplayName("invalid arguments")
	class InvalidArguments
	{
		@Test
		@DisplayName("should reject null reason")
		void shouldRejectNullReason()
		{
			assertThatThrownBy(() -> LocalAccountCreationFailure.of(null))
					.as("null reason should fail fast")
					.isInstanceOf(NullPointerException.class)
					.hasMessage("reason must not be null");
		}
	}
}