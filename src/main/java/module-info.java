module de.gupta.security.janus
{
	exports de.gupta.security.janus.api;
	exports de.gupta.security.janus.api.command;

	exports de.gupta.security.janus.adapter.local;
	exports de.gupta.security.janus.adapter.provider;

	exports de.gupta.security.janus.domain.model.common;
	exports de.gupta.security.janus.domain.model.local;
	exports de.gupta.security.janus.domain.model.provider;
	exports de.gupta.security.janus.domain.model.signin;
	exports de.gupta.security.janus.domain.model.signup;

	requires de.gupta.aletheia;
	requires de.gupta.athena;
}