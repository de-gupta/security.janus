package de.gupta.security.janus.spring.application;

sealed interface ResolutionResult<T> permits Resolved, Unresolved
{
	static <T> ResolutionResult<T> resolved(final T value)
	{
		return new Resolved<>(value);
	}

	static <T> ResolutionResult<T> missing(final String message)
	{
		return new Unresolved<>(ResolutionFailure.of(ResolutionFailureKind.MISSING, message));
	}

	static <T> ResolutionResult<T> ambiguous(final String message)
	{
		return new Unresolved<>(ResolutionFailure.of(ResolutionFailureKind.AMBIGUOUS, message));
	}

	static <T> ResolutionResult<T> incomplete(final String message)
	{
		return new Unresolved<>(ResolutionFailure.of(ResolutionFailureKind.INCOMPLETE, message));
	}
}

enum ResolutionFailureKind
{
	MISSING,
	AMBIGUOUS,
	INCOMPLETE
}

record Resolved<T>(T value) implements ResolutionResult<T>
{
}

record Unresolved<T>(ResolutionFailure failure) implements ResolutionResult<T>
{
}

record ResolutionFailure(ResolutionFailureKind kind, String message)
{
	static ResolutionFailure of(final ResolutionFailureKind kind, final String message)
	{
		return new ResolutionFailure(kind, message);
	}
}