package de.gupta.security.janus.core.domain.model.local;

import de.gupta.security.janus.core.domain.model.common.LocalAccountReference;
import java.util.Objects;

public record LocalAccountCreationSuccess(LocalAccountReference localAccount) implements LocalAccountCreationResult
{
	public static LocalAccountCreationSuccess of(final LocalAccountReference localAccount)
	{
		return new LocalAccountCreationSuccess(localAccount);
	}

	public LocalAccountCreationSuccess
	{
		Objects.requireNonNull(localAccount, "localAccount must not be null");
	}
}