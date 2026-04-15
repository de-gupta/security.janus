module de.gupta.security.janus.core
{
	exports de.gupta.security.janus.core.api;
	exports de.gupta.security.janus.core.api.command;

	exports de.gupta.security.janus.core.adapter.local;
	exports de.gupta.security.janus.core.adapter.provider;

	exports de.gupta.security.janus.core.domain.model.common;
	exports de.gupta.security.janus.core.domain.model.local;
	exports de.gupta.security.janus.core.domain.model.provider;
	exports de.gupta.security.janus.core.domain.model.signin;
	exports de.gupta.security.janus.core.domain.model.signup;

	requires de.gupta.aletheia;
	requires de.gupta.athena;
}