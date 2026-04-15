package de.gupta.security.janus.adapter.local;

import de.gupta.security.janus.domain.model.local.LocalAccountCreationResult;

public interface LocalAccountCreationPort
{
	LocalAccountCreationResult createLocalAccount(LocalAccountCreationCommand command);
}
