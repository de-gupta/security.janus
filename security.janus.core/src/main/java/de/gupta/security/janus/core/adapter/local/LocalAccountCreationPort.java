package de.gupta.security.janus.core.adapter.local;

import de.gupta.security.janus.core.domain.model.local.LocalAccountCreationResult;

public interface LocalAccountCreationPort
{
	LocalAccountCreationResult createLocalAccount(LocalAccountCreationCommand command);
}