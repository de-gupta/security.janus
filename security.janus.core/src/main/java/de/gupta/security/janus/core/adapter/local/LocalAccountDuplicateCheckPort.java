package de.gupta.security.janus.core.adapter.local;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface LocalAccountDuplicateCheckPort
{
	boolean existsByLoginIdentifier(String loginIdentifier);

	static LocalAccountDuplicateCheckPort of(final Predicate<String> duplicateCheck)
	{
		Objects.requireNonNull(duplicateCheck, "duplicateCheck must not be null");

		return duplicateCheck::test;
	}
}
