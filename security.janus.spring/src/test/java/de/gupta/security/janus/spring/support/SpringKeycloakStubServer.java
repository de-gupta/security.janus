package de.gupta.security.janus.spring.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.gupta.security.janus.idp.implementation.keycloak.KeycloakIdentityProviderConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public final class SpringKeycloakStubServer implements AutoCloseable
{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final HttpServer server;
	private String createUserBody = "";
	private String passwordGrantBody = "";

	public SpringKeycloakStubServer() throws IOException
	{
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
	}

	public String baseUrl()
	{
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	public KeycloakIdentityProviderConfiguration configuration(final Clock clock)
	{
		return KeycloakIdentityProviderConfiguration.of(baseUrl(),
				"janus",
				"keycloak",
				"janus-app",
				Optional.of("user-client-secret"),
				"janus-admin",
				Optional.of("admin-client-secret"),
				"master",
				true,
				Optional.of("openid profile email"),
				Duration.ofSeconds(2),
				clock);
	}

	public void stubAdminToken(final String token)
	{
		server.createContext("/realms/master/protocol/openid-connect/token", exchange ->
		{
			final String requestBody = readBody(exchange);
			assertThat(requestBody)
					.contains("grant_type=client_credentials")
					.contains("client_id=janus-admin")
					.contains("client_secret=admin-client-secret");

			writeResponse(exchange, 200, "{\"access_token\":\"" + token + "\"}");
		});
	}

	public void stubCreateUser(final String userId)
	{
		server.createContext("/admin/realms/janus/users", exchange ->
		{
			assertThat(exchange.getRequestMethod()).isEqualTo("POST");
			assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer admin-token");
			createUserBody = readBody(exchange);

			exchange.getResponseHeaders().add("Location", baseUrl() + "/admin/realms/janus/users/" + userId);
			writeResponse(exchange, 201, "");
		});
	}

	public void stubResetPassword(final String userId)
	{
		server.createContext("/admin/realms/janus/users/" + userId + "/reset-password", exchange ->
		{
			assertThat(exchange.getRequestMethod()).isEqualTo("PUT");
			assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer admin-token");
			assertThat(readBody(exchange))
					.contains("\"type\":\"password\"")
					.contains("\"value\":\"correct horse battery staple\"")
					.contains("\"temporary\":false");

			writeResponse(exchange, 204, "");
		});
	}

	public void stubPasswordGrantSuccess(final String subject, final String email) throws IOException
	{
		final Map<String, Object> idClaims = new LinkedHashMap<>();
		idClaims.put("sub", subject);
		idClaims.put("preferred_username", email);
		idClaims.put("email", email);

		final Map<String, Object> response = new LinkedHashMap<>();
		response.put("access_token", "access-token");
		response.put("refresh_token", "refresh-token");
		response.put("id_token", jwt(idClaims));
		response.put("session_state", "session-123");
		response.put("expires_in", 3600);
		response.put("scope", "openid profile email");
		response.put("token_type", "Bearer");

		final String responseBody = OBJECT_MAPPER.writeValueAsString(response);
		server.createContext("/realms/janus/protocol/openid-connect/token", exchange ->
		{
			assertThat(exchange.getRequestMethod()).isEqualTo("POST");
			passwordGrantBody = readBody(exchange);
			writeResponse(exchange, 200, responseBody);
		});
	}

	public String lastCreateUserBody()
	{
		return createUserBody;
	}

	public String lastPasswordGrantBody()
	{
		return passwordGrantBody;
	}

	@Override
	public void close()
	{
		server.stop(0);
	}

	private static String jwt(final Map<String, Object> claims) throws IOException
	{
		final String header = Base64.getUrlEncoder()
		                            .withoutPadding()
		                            .encodeToString(
											"{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
		final String payload = Base64.getUrlEncoder()
		                             .withoutPadding()
		                             .encodeToString(OBJECT_MAPPER.writeValueAsBytes(claims));
		return header + "." + payload + ".signature";
	}

	private static String readBody(final HttpExchange exchange) throws IOException
	{
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private static void writeResponse(final HttpExchange exchange,
	                                  final int status,
	                                  final String body) throws IOException
	{
		final byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, payload.length);
		try (exchange; OutputStream outputStream = exchange.getResponseBody())
		{
			outputStream.write(payload);
		}
	}
}