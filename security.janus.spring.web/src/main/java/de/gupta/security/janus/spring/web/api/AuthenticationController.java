package de.gupta.security.janus.spring.web.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthenticationController<SignupRequest, SigninRequest, SignupBody, SigninBody>
{
	@PostMapping("/signup")
	ResponseEntity<SignupBody> signup(@RequestBody @Valid SignupRequest request);

	@PostMapping("/signin")
	ResponseEntity<SigninBody> signin(@RequestBody @Valid SigninRequest request);
}