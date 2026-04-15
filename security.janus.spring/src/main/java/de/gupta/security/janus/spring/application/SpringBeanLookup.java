package de.gupta.security.janus.spring.application;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class SpringBeanLookup
{
	private final ListableBeanFactory beanFactory;

	<T> List<String> beanNames(final Class<T> type)
	{
		return sortedBeanNames(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type));
	}

	List<String> beanNames(final ResolvableType type)
	{
		return sortedBeanNames(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type));
	}

	<T> T bean(final String beanName, final Class<T> type)
	{
		return beanFactory.getBean(beanName, type);
	}

	Object bean(final String beanName)
	{
		return beanFactory.getBean(beanName);
	}

	boolean hasAnyBean(final Class<?> type)
	{
		return !beanNames(type).isEmpty();
	}

	boolean hasAnyBean(final ResolvableType type)
	{
		return !beanNames(type).isEmpty();
	}

	private List<String> sortedBeanNames(final String[] beanNames)
	{
		return Arrays.stream(beanNames)
		             .sorted()
		             .collect(Collectors.toList());
	}

	SpringBeanLookup(final ListableBeanFactory beanFactory)
	{
		this.beanFactory = beanFactory;
	}
}