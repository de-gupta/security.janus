package de.gupta.security.janus.core.domain.model.signup;

public record SignupCompletion(boolean providerAccountCreated,
                               boolean localAccountCreated,
                               boolean sessionEstablished)
{
	public static SignupCompletion completed(final boolean sessionEstablished)
	{
		return new SignupCompletion(true, true, sessionEstablished);
	}
}