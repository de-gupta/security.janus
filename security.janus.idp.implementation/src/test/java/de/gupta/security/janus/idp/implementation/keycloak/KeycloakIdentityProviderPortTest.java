package de.gupta.security.janus.idp.implementation.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.gupta.security.janus.core.adapter.provider.SigninProviderCommand;
import de.gupta.security.janus.core.adapter.provider.SignupProviderCommand;
import de.gupta.security.janus.core.api.command.SignupProfileAttributes;
import de.gupta.security.janus.core.domain.model.provider.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakIdentityProviderPort")
class KeycloakIdentityProviderPortTest
{
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC);

	private static String successfulTokenResponse(final String subject, final String email) throws IOException
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
		return OBJECT_MAPPER.writeValueAsString(response);
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

	private static final class Fixtures
	{
		private static KeycloakIdentityProviderConfiguration configuration(final String serverUrl)
		{
			return KeycloakIdentityProviderConfiguration.of(serverUrl,
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
					FIXED_CLOCK);
		}

		private static SignupProviderCommand signupCommand()
		{
			return SignupProviderCommand.of("ada@example.com",
					"correct horse battery staple",
					SignupProfileAttributes.of(Optional.of("ada@example.com"),
							Optional.of("Ada"),
							Optional.of("Lovelace"),
							Optional.of("Countess")),
					"keycloak");
		}

		private static SigninProviderCommand signinCommand()
		{
			return SigninProviderCommand.of("ada@example.com", "correct horse battery staple", "keycloak");
		}
	}

	private static final class StubKeycloakServer implements AutoCloseable
	{
		private final HttpServer server;
		private String createUserBody = "";
		private String passwordGrantBody = "";

		@Override
		public void close()
		{
			server.stop(0);
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

		private String baseUrl()
		{
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		private void stubAdminToken(final String token)
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

		private void stubCreateUser(final String userId)
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

		private void stubCreateUserConflict()
		{
			server.createContext("/admin/realms/janus/users", exchange ->
			{
				assertThat(exchange.getRequestMethod()).isEqualTo("POST");
				writeResponse(exchange, 409, "{\"errorMessage\":\"User exists\"}");
			});
		}

		private void stubResetPassword(final String userId)
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

		private void stubPasswordGrant(final String responseBody)
		{
			server.createContext("/realms/janus/protocol/openid-connect/token", exchange ->
			{
				assertThat(exchange.getRequestMethod()).isEqualTo("POST");
				passwordGrantBody = readBody(exchange);
				writeResponse(exchange, 200, responseBody);
			});
		}

		private void stubPasswordGrantInvalidCredentials()
		{
			server.createContext("/realms/janus/protocol/openid-connect/token", exchange ->
					writeResponse(exchange, 400,
							"{\"error\":\"invalid_grant\",\"error_description\":\"Bad credentials\"}"));
		}

		private String lastCreateUserBody()
		{
			return createUserBody;
		}

		private String lastPasswordGrantBody()
		{
			return passwordGrantBody;
		}

		private StubKeycloakServer() throws IOException
		{
			server = HttpServer.create(new InetSocketAddress(0), 0);
			server.start();
		}
	}

	@Nested
	@DisplayName("signup")
	class Signup
	{
		@Test
		@DisplayName("should sign up through Keycloak admin APIs and return a provider session")
		void shouldSignUpThroughKeycloakAdminApisAndReturnAProviderSession() throws Exception
		{
			try (StubKeycloakServer server = new StubKeycloakServer())
			{
				server.stubAdminToken("admin-token");
				server.stubCreateUser("keycloak-user-1");
				server.stubResetPassword("keycloak-user-1");
				server.stubPasswordGrant(successfulTokenResponse("keycloak-user-1", "ada@example.com"));

				final KeycloakIdentityProviderPort adapter =
						KeycloakIdentityProviderPort.create(Fixtures.configuration(server.baseUrl()));

				assertThat(adapter.signup(Fixtures.signupCommand()))
						.isInstanceOf(ProviderSignupSuccess.class)
						.extracting(ProviderSignupSuccess.class::cast)
						.satisfies(success ->
						{
							assertThat(success.providerIdentity().externalSubject()).isEqualTo("keycloak-user-1");
							assertThat(success.providerIdentity().provider()).isEqualTo("keycloak");
							assertThat(success.providerIdentity().metadata())
									.containsEntry("realm", "janus")
									.containsEntry("username", "ada@example.com")
									.containsEntry("email", "ada@example.com");
							assertThat(success.providerSession()).isPresent();
							assertThat(success.providerSession().orElseThrow().sessionId()).contains("session-123");
							assertThat(success.providerSession().orElseThrow().accessToken()).contains("access-token");
							assertThat(success.providerSession().orElseThrow().refreshToken()).contains(
									"refresh-token");
							assertThat(success.providerSession().orElseThrow().idToken()).isPresent();
							assertThat(success.providerSession().orElseThrow().issuedAt()).contains(
									FIXED_CLOCK.instant());
							assertThat(success.providerSession().orElseThrow().expiresAt())
									.contains(FIXED_CLOCK.instant().plusSeconds(3600));
							assertThat(success.providerSession().orElseThrow().metadata())
									.containsEntry("scope", "openid profile email")
									.containsEntry("tokenType", "Bearer");
						});

				assertThat(server.lastCreateUserBody())
						.contains("\"username\":\"ada@example.com\"")
						.contains("\"email\":\"ada@example.com\"")
						.contains("\"firstName\":\"Ada\"")
						.contains("\"lastName\":\"Lovelace\"");
				assertThat(server.lastPasswordGrantBody())
						.contains("grant_type=password")
						.contains("client_id=janus-app")
						.contains("client_secret=user-client-secret")
						.contains("username=ada%40example.com")
						.contains("password=correct+horse+battery+staple")
						.contains("scope=openid+profile+email");
			}
		}

		@Test
		@DisplayName("should map duplicate Keycloak users to duplicate identity failures")
		void shouldMapDuplicateKeycloakUsersToDuplicateIdentityFailures() throws Exception
		{
			try (StubKeycloakServer server = new StubKeycloakServer())
			{
				server.stubAdminToken("admin-token");
				server.stubCreateUserConflict();

				final KeycloakIdentityProviderPort adapter =
						KeycloakIdentityProviderPort.create(Fixtures.configuration(server.baseUrl()));

				assertThat(adapter.signup(Fixtures.signupCommand()))
						.isInstanceOf(ProviderSignupFailure.class)
						.extracting(ProviderSignupFailure.class::cast)
						.satisfies(failure ->
						{
							assertThat(failure.reason()).isEqualTo(ProviderSignupFailureReason.DUPLICATE_IDENTITY);
							assertThat(failure.details())
									.hasValueSatisfying(details -> assertThat(details).contains("exists"));
						});
			}
		}
	}

	@Nested
	@DisplayName("signin")
	class Signin
	{
		@Test
		@DisplayName("should sign in through the Keycloak token endpoint")
		void shouldSignInThroughTheKeycloakTokenEndpoint() throws Exception
		{
			try (StubKeycloakServer server = new StubKeycloakServer())
			{
				server.stubPasswordGrant(successfulTokenResponse("keycloak-user-2", "ada@example.com"));

				final KeycloakIdentityProviderPort adapter =
						KeycloakIdentityProviderPort.create(Fixtures.configuration(server.baseUrl()));

				assertThat(adapter.signin(Fixtures.signinCommand()))
						.isInstanceOf(ProviderSigninSuccess.class)
						.extracting(ProviderSigninSuccess.class::cast)
						.satisfies(success ->
						{
							assertThat(success.providerIdentity().externalSubject()).isEqualTo("keycloak-user-2");
							assertThat(success.providerIdentity().provider()).isEqualTo("keycloak");
							assertThat(success.providerIdentity().metadata())
									.containsEntry("realm", "janus")
									.containsEntry("username", "ada@example.com")
									.containsEntry("email", "ada@example.com");
							assertThat(success.providerSession().sessionId()).contains("session-123");
							assertThat(success.providerSession().expiresAt())
									.contains(FIXED_CLOCK.instant().plusSeconds(3600));
						});
			}
		}

		@Test
		@DisplayName("should map invalid Keycloak credentials to invalid credentials failures")
		void shouldMapInvalidKeycloakCredentialsToInvalidCredentialsFailures() throws Exception
		{
			try (StubKeycloakServer server = new StubKeycloakServer())
			{
				server.stubPasswordGrantInvalidCredentials();

				final KeycloakIdentityProviderPort adapter =
						KeycloakIdentityProviderPort.create(Fixtures.configuration(server.baseUrl()));

				assertThat(adapter.signin(Fixtures.signinCommand()))
						.isInstanceOf(ProviderSigninFailure.class)
						.extracting(ProviderSigninFailure.class::cast)
						.satisfies(failure ->
						{
							assertThat(failure.reason()).isEqualTo(ProviderSigninFailureReason.INVALID_CREDENTIALS);
							assertThat(failure.details()).contains("Bad credentials");
						});
			}
		}

		@Test
		@DisplayName("should reject unsupported providers before calling Keycloak")
		void shouldRejectUnsupportedProvidersBeforeCallingKeycloak()
		{
			final KeycloakIdentityProviderPort adapter =
					KeycloakIdentityProviderPort.create(Fixtures.configuration("http://127.0.0.1:65535"));

			assertThat(adapter.signin(SigninProviderCommand.of("ada@example.com",
					"correct horse battery staple",
					"github")))
					.isInstanceOf(ProviderSigninFailure.class)
					.extracting(ProviderSigninFailure.class::cast)
					.satisfies(failure -> assertThat(failure.reason()).isEqualTo(
							ProviderSigninFailureReason.UNSUPPORTED_PROVIDER));
		}
	}
}