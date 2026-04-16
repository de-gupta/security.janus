package de.gupta.security.janus.idp.implementation.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

final class KeycloakApiClient
{
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final KeycloakIdentityProviderConfiguration configuration;

	KeycloakUserCreationResult createUser(final SignupProviderCommand command) throws KeycloakClientException
	{
		final String adminToken = adminAccessToken();
		final CreateUserRequest requestBody = CreateUserRequest.from(command);
		final HttpRequest request = HttpRequest.newBuilder(adminUsersEndpoint())
		                                       .timeout(REQUEST_TIMEOUT)
		                                       .header("Authorization", "Bearer " + adminToken)
		                                       .header("Content-Type", "application/json")
		                                       .POST(HttpRequest.BodyPublishers.ofString(writeJson(requestBody)))
		                                       .build();
		final HttpResponse<String> response = send(request);

		return switch (response.statusCode())
		{
			case 201 -> new KeycloakUserCreationResult(extractUserId(response),
					command.loginIdentifier(),
					command.profileAttributes().email());
			case 409 -> throw new KeycloakClientException(KeycloakFailureCategory.DUPLICATE_IDENTITY,
					"Keycloak user already exists");
			case 400 -> throw new KeycloakClientException(KeycloakFailureCategory.MALFORMED_COMMAND,
					messageOrDefault(response.body(), "Keycloak rejected the signup payload"));
			case 401, 403 -> throw new KeycloakClientException(KeycloakFailureCategory.PROVIDER_REJECTED,
					"Keycloak admin client is not authorized to create users");
			default -> throw mapUnexpected(response, "create Keycloak user");
		};
	}

	void setPassword(final String userId, final String rawSecret) throws KeycloakClientException
	{
		final String adminToken = adminAccessToken();
		final HttpRequest request = HttpRequest.newBuilder(resetPasswordEndpoint(userId))
		                                       .timeout(REQUEST_TIMEOUT)
		                                       .header("Authorization", "Bearer " + adminToken)
		                                       .header("Content-Type", "application/json")
		                                       .PUT(HttpRequest.BodyPublishers.ofString(
													   writeJson(new CredentialRepresentation(
															   "password",
															   rawSecret,
															   false))))
		                                       .build();
		final HttpResponse<String> response = send(request);

		switch (response.statusCode())
		{
			case 204:
				return;
			case 400:
				throw new KeycloakClientException(KeycloakFailureCategory.MALFORMED_COMMAND,
						messageOrDefault(response.body(), "Keycloak rejected the password payload"));
			case 401, 403:
				throw new KeycloakClientException(KeycloakFailureCategory.PROVIDER_REJECTED,
						"Keycloak admin client is not authorized to reset passwords");
			default:
				throw mapUnexpected(response, "set Keycloak password");
		}
	}

	KeycloakTokenResponse passwordGrant(final String username, final String rawSecret) throws KeycloakClientException
	{
		final Map<String, String> parameters = new LinkedHashMap<>();
		parameters.put("grant_type", "password");
		parameters.put("client_id", configuration.userClientId());
		parameters.put("username", username);
		parameters.put("password", rawSecret);
		configuration.userClientSecret().ifPresent(secret -> parameters.put("client_secret", secret));
		configuration.scope().ifPresent(scope -> parameters.put("scope", scope));

		final HttpRequest request = formPost(tokenEndpoint(configuration.realm()), parameters);
		final HttpResponse<String> response = send(request);

		return switch (response.statusCode())
		{
			case 200 -> enrichTokenResponse(readJson(response.body(), TokenEndpointResponse.class));
			case 400 -> throw invalidCredentialsOrMalformed(response.body());
			case 401, 403 -> throw new KeycloakClientException(KeycloakFailureCategory.PROVIDER_REJECTED,
					"Keycloak rejected the signin request");
			default -> throw mapUnexpected(response, "exchange Keycloak password grant");
		};
	}

	private String adminAccessToken() throws KeycloakClientException
	{
		final Map<String, String> parameters = new LinkedHashMap<>();
		parameters.put("grant_type", "client_credentials");
		parameters.put("client_id", configuration.adminClientId());
		configuration.adminClientSecret().ifPresent(secret -> parameters.put("client_secret", secret));

		final HttpRequest request = formPost(tokenEndpoint(configuration.adminRealm()), parameters);
		final HttpResponse<String> response = send(request);

		return switch (response.statusCode())
		{
			case 200 -> requiredValue(readJson(response.body(), TokenEndpointResponse.class).accessToken(),
					"Keycloak admin token response did not include an access token");
			case 400 -> throw new KeycloakClientException(KeycloakFailureCategory.MALFORMED_COMMAND,
					messageOrDefault(response.body(), "Keycloak rejected the admin token request"));
			case 401, 403 -> throw new KeycloakClientException(KeycloakFailureCategory.PROVIDER_REJECTED,
					"Keycloak admin client authentication failed");
			default -> throw mapUnexpected(response, "obtain Keycloak admin access token");
		};
	}

	private HttpRequest formPost(final URI uri, final Map<String, String> parameters)
	{
		return HttpRequest.newBuilder(uri)
		                  .timeout(REQUEST_TIMEOUT)
		                  .header("Content-Type", "application/x-www-form-urlencoded")
		                  .POST(HttpRequest.BodyPublishers.ofString(formEncode(parameters)))
		                  .build();
	}

	private HttpResponse<String> send(final HttpRequest request) throws KeycloakClientException
	{
		try
		{
			return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		}
		catch (final IOException | InterruptedException exception)
		{
			if (exception instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
			throw new KeycloakClientException(KeycloakFailureCategory.PROVIDER_UNAVAILABLE,
					"Unable to reach Keycloak", exception);
		}
	}

	private KeycloakTokenResponse enrichTokenResponse(final TokenEndpointResponse tokenResponse)
	{
		final Optional<String> accessToken = optionalString(tokenResponse.accessToken());
		final Optional<String> idToken = optionalString(tokenResponse.idToken());
		final Map<String, Object> claims = idToken.map(this::decodeJwtClaims)
		                                          .orElseGet(() -> accessToken.map(this::decodeJwtClaims)
		                                                                      .orElse(Map.of()));
		return new KeycloakTokenResponse(accessToken,
				Optional.ofNullable(tokenResponse.expiresIn()),
				optionalString(tokenResponse.refreshToken()),
				idToken,
				optionalString(tokenResponse.sessionState()),
				optionalString(tokenResponse.scope()),
				optionalString(tokenResponse.tokenType()),
				optionalString(claims.get("sub")),
				optionalString(claims.get("preferred_username")),
				optionalString(claims.get("email")));
	}

	private Map<String, Object> decodeJwtClaims(final String jwt)
	{
		final String[] parts = jwt.split("\\.");
		if (parts.length < 2)
		{
			return Map.of();
		}

		try
		{
			final byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
			return objectMapper.readValue(payload, new TypeReference<>()
			{
			});
		}
		catch (final IllegalArgumentException | IOException exception)
		{
			return Map.of();
		}
	}

	private KeycloakClientException invalidCredentialsOrMalformed(final String responseBody)
	{
		if (responseBody == null || responseBody.isBlank())
		{
			return new KeycloakClientException(KeycloakFailureCategory.INVALID_CREDENTIALS,
					"Invalid Keycloak credentials");
		}

		try
		{
			final TokenErrorResponse error = objectMapper.readValue(responseBody, TokenErrorResponse.class);
			if ("invalid_grant".equals(error.error()))
			{
				return new KeycloakClientException(KeycloakFailureCategory.INVALID_CREDENTIALS,
						errorDescriptionOrDefault(error.errorDescription(), "Invalid Keycloak credentials"));
			}
			return new KeycloakClientException(KeycloakFailureCategory.MALFORMED_COMMAND,
					errorDescriptionOrDefault(error.errorDescription(), "Keycloak rejected the signin payload"));
		}
		catch (final IOException exception)
		{
			return new KeycloakClientException(KeycloakFailureCategory.INVALID_CREDENTIALS,
					"Invalid Keycloak credentials");
		}
	}

	private KeycloakClientException mapUnexpected(final HttpResponse<String> response,
	                                              final String operation)
	{
		if (response.statusCode() >= 500)
		{
			return new KeycloakClientException(KeycloakFailureCategory.PROVIDER_UNAVAILABLE,
					"Keycloak was unavailable while trying to " + operation);
		}

		return new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR,
				"Unexpected Keycloak response while trying to " + operation + ": HTTP " + response.statusCode());
	}

	private String extractUserId(final HttpResponse<String> response) throws KeycloakClientException
	{
		final Optional<String> location = response.headers().firstValue("Location");
		if (location.isEmpty())
		{
			throw new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR,
					"Keycloak create user response did not contain a Location header");
		}

		final String path = URI.create(location.get()).getPath();
		final int lastSlash = path.lastIndexOf('/');
		if (lastSlash < 0 || lastSlash == path.length() - 1)
		{
			throw new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR,
					"Unable to extract Keycloak user id from Location header");
		}

		return path.substring(lastSlash + 1);
	}

	private String writeJson(final Object value) throws KeycloakClientException
	{
		try
		{
			return objectMapper.writeValueAsString(value);
		}
		catch (final IOException exception)
		{
			throw new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR,
					"Unable to serialize Keycloak request payload", exception);
		}
	}

	private <T> T readJson(final String body, final Class<T> type) throws KeycloakClientException
	{
		try
		{
			return objectMapper.readValue(body, type);
		}
		catch (final IOException exception)
		{
			throw new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR,
					"Unable to parse Keycloak response payload", exception);
		}
	}

	private String messageOrDefault(final String body, final String fallback)
	{
		if (body == null || body.isBlank())
		{
			return fallback;
		}

		return body;
	}

	private String requiredValue(final String value, final String message) throws KeycloakClientException
	{
		return optionalString(value)
				.orElseThrow(() -> new KeycloakClientException(KeycloakFailureCategory.INTERNAL_ERROR, message));
	}

	private Optional<String> optionalString(final Object value)
	{
		return Optional.ofNullable(value)
		               .map(Object::toString)
		               .map(String::trim)
		               .filter(candidate -> !candidate.isBlank());
	}

	private String errorDescriptionOrDefault(final String value, final String fallback)
	{
		return optionalString(value).orElse(fallback);
	}

	private String formEncode(final Map<String, String> parameters)
	{
		return parameters.entrySet()
		                 .stream()
		                 .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
		                 .collect(Collectors.joining("&"));
	}

	private String encode(final String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private URI tokenEndpoint(final String realm)
	{
		return URI.create(trimTrailingSlash(configuration.serverUrl()) +
				"/realms/" + realm + "/protocol/openid-connect/token");
	}

	private URI adminUsersEndpoint()
	{
		return URI.create(trimTrailingSlash(configuration.serverUrl()) +
				"/admin/realms/" + configuration.realm() + "/users");
	}

	private URI resetPasswordEndpoint(final String userId)
	{
		return URI.create(trimTrailingSlash(configuration.serverUrl()) +
				"/admin/realms/" + configuration.realm() + "/users/" + userId + "/reset-password");
	}

	private String trimTrailingSlash(final String value)
	{
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	KeycloakApiClient(final HttpClient httpClient,
	                  final ObjectMapper objectMapper,
	                  final KeycloakIdentityProviderConfiguration configuration)
	{
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.configuration = configuration;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenEndpointResponse(String access_token,
	                                     Long expires_in,
	                                     String refresh_token,
	                                     String id_token,
	                                     String session_state,
	                                     String scope,
	                                     String token_type)
	{
		private String accessToken()
		{
			return access_token;
		}

		private Long expiresIn()
		{
			return expires_in;
		}

		private String refreshToken()
		{
			return refresh_token;
		}

		private String idToken()
		{
			return id_token;
		}

		private String sessionState()
		{
			return session_state;
		}

		private String tokenType()
		{
			return token_type;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenErrorResponse(String error, String error_description)
	{
		private String errorDescription()
		{
			return error_description;
		}
	}

	private record CredentialRepresentation(String type, String value, boolean temporary)
	{
	}

	private record CreateUserRequest(String username,
	                                 String email,
	                                 String firstName,
	                                 String lastName,
	                                 boolean enabled)
	{
		private static CreateUserRequest from(final SignupProviderCommand command)
		{
			Objects.requireNonNull(command, "command must not be null");
			return new CreateUserRequest(command.loginIdentifier(),
					command.profileAttributes().email().orElse(null),
					command.profileAttributes().firstName().orElse(null),
					command.profileAttributes().lastName().orElse(null),
					true);
		}
	}
}