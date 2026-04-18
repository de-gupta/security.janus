package de.gupta.security.janus.spring.web.api;

import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticationController contract")
class AuthenticationControllerContractTest
{
	@Test
	@DisplayName("should declare signup endpoint metadata")
	void shouldDeclareSignupEndpointMetadata() throws Exception
	{
		final Method method = AuthenticationController.class.getMethod("signup", Object.class);

		assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/signup");
		assertThat(method.getParameterCount()).isEqualTo(1);
		assertThat(method.getParameterTypes()[0]).isEqualTo(Object.class);

		final Parameter parameter = method.getParameters()[0];
		assertThat(parameter.isAnnotationPresent(RequestBody.class)).isTrue();
		assertThat(parameter.isAnnotationPresent(Valid.class)).isTrue();
	}

	@Test
	@DisplayName("should declare signin endpoint metadata")
	void shouldDeclareSigninEndpointMetadata() throws Exception
	{
		final Method method = AuthenticationController.class.getMethod("signin", Object.class);

		assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly("/signin");
		assertThat(method.getParameterCount()).isEqualTo(1);
		assertThat(method.getParameterTypes()[0]).isEqualTo(Object.class);

		final Parameter parameter = method.getParameters()[0];
		assertThat(parameter.isAnnotationPresent(RequestBody.class)).isTrue();
		assertThat(parameter.isAnnotationPresent(Valid.class)).isTrue();
	}
}
