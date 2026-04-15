package de.gupta.security.janus.spring.configuration;

import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.AuthenticationServiceFactory;
import de.gupta.security.janus.spring.application.AuthenticationConfigurationAssembler;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JanusAuthenticationSpringConfiguration
{
	@Bean
	public AuthenticationService janusAuthenticationService(final ListableBeanFactory beanFactory)
	{
		return AuthenticationServiceFactory.create(AuthenticationConfigurationAssembler.create(beanFactory)
		                                                                               .assembleOrThrow());
	}
}