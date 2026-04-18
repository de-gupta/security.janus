package de.gupta.security.janus.spring.web.facade;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public record AuthenticationWebResponse<T>(HttpStatus status, T body)
{
	public static <T> AuthenticationWebResponse<T> of(final HttpStatus status, final T body)
	{
		return new AuthenticationWebResponse<>(status, body);
	}

	public AuthenticationWebResponse
	{
		Objects.requireNonNull(status, "status must not be null");
	}
}