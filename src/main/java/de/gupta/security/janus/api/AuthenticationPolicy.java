package de.gupta.security.janus.api;

public record AuthenticationPolicy(boolean allowUnlinkedLocalSignin)
{
	public static AuthenticationPolicy defaults()
	{
		return new AuthenticationPolicy(false);
	}

	public static AuthenticationPolicy allowingUnlinkedLocalSignin()
	{
		return new AuthenticationPolicy(true);
	}
}