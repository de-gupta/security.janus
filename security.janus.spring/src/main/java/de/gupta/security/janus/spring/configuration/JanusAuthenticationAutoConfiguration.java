package de.gupta.security.janus.spring.configuration;

import de.gupta.security.janus.core.api.AuthenticationService;
import de.gupta.security.janus.core.api.AuthenticationServiceFactory;
import de.gupta.security.janus.spring.application.AuthenticationConfigurationAssembler;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

@AutoConfiguration
@ConditionalOnClass(AuthenticationService.class)
public class JanusAuthenticationAutoConfiguration
{
	@Bean
	@ConditionalOnMissingBean(AuthenticationService.class)
	@Conditional(JanusAuthenticationCandidatesPresentCondition.class)
	public AuthenticationService janusAuthenticationService(final ListableBeanFactory beanFactory)
	{
		return AuthenticationServiceFactory.create(AuthenticationConfigurationAssembler.create(beanFactory)
		                                                                               .assembleOrThrow());
	}
}