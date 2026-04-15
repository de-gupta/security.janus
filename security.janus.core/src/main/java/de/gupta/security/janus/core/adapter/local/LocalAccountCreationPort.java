package de.gupta.security.janus.core.adapter.local;

import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface LocalAccountCreationPort
{
	LocalAccountCreationResult createLocalAccount(LocalAccountCreationCommand command);

	static LocalAccountCreationPort of(
			final Function<LocalAccountCreationCommand, LocalAccountCreationResult> createHandler)
	{
		Objects.requireNonNull(createHandler, "createHandler must not be null");

		return createHandler::apply;
	}
}