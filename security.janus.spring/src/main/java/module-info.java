open module de.gupta.security.janus.spring
{
	exports de.gupta.security.janus.spring.configuration;

	requires transitive de.gupta.security.janus.core;

	requires spring.beans;
	requires spring.boot.autoconfigure;
	requires spring.context;
	requires spring.core;
}