package de.gupta.security.janus.spring.configuration;

import de.gupta.security.janus.spring.application.AuthenticationConfigurationCandidateProbe;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

final class JanusAuthenticationCandidatesPresentCondition implements Condition
{
	@Override
	public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata)
	{
		if (!(context.getBeanFactory() instanceof ListableBeanFactory listableBeanFactory))
		{
			return false;
		}

		return AuthenticationConfigurationCandidateProbe.hasRequiredCandidates(listableBeanFactory);
	}
}