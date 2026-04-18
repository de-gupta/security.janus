module de.gupta.security.janus.spring.web
{
	exports de.gupta.security.janus.spring.web.api;
	exports de.gupta.security.janus.spring.web.facade;
	exports de.gupta.security.janus.spring.web.adapter;

	requires transitive de.gupta.security.janus.core;
	requires transitive jakarta.validation;
	requires transitive spring.web;
}