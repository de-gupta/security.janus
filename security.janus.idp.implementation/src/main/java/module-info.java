module de.gupta.security.janus.idp.implementation
{
	exports de.gupta.security.janus.idp.implementation.keycloak;

	requires transitive de.gupta.security.janus.core;
	requires com.fasterxml.jackson.databind;
	requires jdk.httpserver;
	requires java.net.http;
}
