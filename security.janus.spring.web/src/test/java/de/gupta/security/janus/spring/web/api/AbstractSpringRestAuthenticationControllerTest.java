package de.gupta.security.janus.spring.web.api;

import de.gupta.security.janus.spring.web.facade.AuthenticationWebFacade;
import de.gupta.security.janus.spring.web.facade.AuthenticationWebResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AbstractSpringRestAuthenticationController")
class AbstractSpringRestAuthenticationControllerTest
{
	private static final class RecordingFacade
			implements AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody>
	{
		private final AuthenticationWebResponse<SignupBody> signupResponse =
				AuthenticationWebResponse.of(HttpStatus.CREATED, new SignupBody("signup-ok"));
		private final AuthenticationWebResponse<SigninBody> signinResponse =
				AuthenticationWebResponse.of(HttpStatus.OK, new SigninBody("signin-ok"));
		private SignupRequest lastSignupRequest;
		private SigninRequest lastSigninRequest;

		@Override
		public AuthenticationWebResponse<SignupBody> signup(final SignupRequest request)
		{
			lastSignupRequest = request;
			return signupResponse;
		}

		@Override
		public AuthenticationWebResponse<SigninBody> signin(final SigninRequest request)
		{
			lastSigninRequest = request;
			return signinResponse;
		}
	}

	private static final class TestController
			extends AbstractSpringRestAuthenticationController<SignupRequest, SigninRequest, SignupBody, SigninBody>
	{
		private TestController(
				final AuthenticationWebFacade<SignupRequest, SigninRequest, SignupBody, SigninBody> facade)
		{
			super(facade);
		}
	}

	private record SignupRequest(String username, String password)
	{
	}

	private record SigninRequest(String username, String password)
	{
	}

	private record SignupBody(String status)
	{
	}

	private record SigninBody(String status)
	{
	}

	@Nested
	@DisplayName("signup")
	class Signup
	{
		@Test
		@DisplayName("should convert signup facade responses into ResponseEntity")
		void shouldConvertSignupFacadeResponsesIntoResponseEntity()
		{
			final RecordingFacade facade = new RecordingFacade();
			final TestController controller = new TestController(facade);
			final SignupRequest request = new SignupRequest("ada@example.com", "secret");

			final ResponseEntity<SignupBody> response = controller.signup(request);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			assertThat(response.getBody()).isEqualTo(new SignupBody("signup-ok"));
			assertThat(facade.lastSignupRequest).isEqualTo(request);
		}
	}

	@Nested
	@DisplayName("signin")
	class Signin
	{
		@Test
		@DisplayName("should convert signin facade responses into ResponseEntity")
		void shouldConvertSigninFacadeResponsesIntoResponseEntity()
		{
			final RecordingFacade facade = new RecordingFacade();
			final TestController controller = new TestController(facade);
			final SigninRequest request = new SigninRequest("ada@example.com", "secret");

			final ResponseEntity<SigninBody> response = controller.signin(request);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo(new SigninBody("signin-ok"));
			assertThat(facade.lastSigninRequest).isEqualTo(request);
		}
	}
}