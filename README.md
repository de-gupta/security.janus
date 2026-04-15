# security.janus

`security.janus` is a core authentication orchestration library for signup and signin.

It is intentionally narrow:

- no Spring controllers yet
- no persistence implementation
- no refresh-token flow
- no password reset, MFA, logout, or email verification

You bring your own local account store and your own identity provider implementation. Janus coordinates the flow and returns rich, typed results.

## What You Provide

As a consumer, you plug Janus into your application by implementing a few ports.

### Provider adapter

Implement `IdentityProviderPort` when Janus should talk to an auth provider such as:

- local auth
- Supabase
- Auth0
- another IdP

Janus calls:

- `signup(SignupProviderCommand)`
- `signin(SigninProviderCommand)`

Your adapter returns normalized provider results from `domain.model.provider`, not raw vendor DTOs.

### Local account adapters

Implement the local account ports for your own application model:

- `LocalAccountLookupPort`
- `LocalAccountCreationPort`
- optionally `LocalAccountDuplicateCheckPort`

These ports let Janus:

- check whether a local account already exists
- create a local account after provider signup succeeds
- resolve a local account from provider subject data during signin

Janus does not require your domain user/entity to implement a big shared interface. It only asks for narrow auth-facing projections such as `LocalAccountIdentityView`.

## What You Get

Janus gives you a configured `AuthenticationService`.

```java
AuthenticationService service = AuthenticationServiceFactory.create(
        AuthenticationConfiguration.of(
                identityProviderPort,
                localAccountLookupPort,
                localAccountCreationPort,
                localAccountDuplicateCheckPort,
                AuthenticationPolicy.defaults(),
                Clock.systemUTC()
        )
);
```

You then call:

```java
SignupResult signupResult = service.signup(signupCommand);
SigninResult signinResult = service.signin(signinCommand);
```

Both results are sealed, typed models. Janus does not collapse outcomes into booleans or exceptions for normal control flow.

## Signup Flow

Default behavior is provider-first:

1. validate the incoming signup command
2. optionally check for obvious local duplicates
3. call your `IdentityProviderPort.signup(...)`
4. if provider signup succeeds, call your `LocalAccountCreationPort`
5. return a typed `SignupResult`

If provider signup succeeds but local account creation fails, Janus returns `SignupFailure` with reason `RECONCILIATION_REQUIRED` and includes the created `ProviderIdentity` so your application can repair or compensate deliberately.

## Signin Flow

Signin is provider-driven:

1. validate the incoming signin command
2. call your `IdentityProviderPort.signin(...)`
3. if provider signin succeeds, resolve the linked local account through `LocalAccountLookupPort`
4. return a typed `SigninResult`

By default, Janus expects a linked local account. If you want provider-authenticated but unlinked signin to succeed, use `AuthenticationPolicy.allowingUnlinkedLocalSignin()`.

## Commands You Send In

Public commands live in `de.gupta.security.janus.api.command`.

### Signup

Use `SignupCommand` and `SignupProfileAttributes`.

```java
SignupCommand command = SignupCommand.of(
        "alice@example.com",
        "secret",
        SignupProfileAttributes.of(
                Optional.of("alice@example.com"),
                Optional.of("Alice"),
                Optional.of("Example"),
                Optional.of("Alice Example")
        ),
        "supabase"
);
```

### Signin

Use `SigninCommand`.

```java
SigninCommand command = SigninCommand.of(
        "alice@example.com",
        "secret",
        "supabase"
);
```

## Results You Get Back

### Signup results

`SignupResult` is sealed:

- `SignupSuccess`
- `SignupFailure`

`SignupSuccess` gives you:

- `ProviderIdentity`
- `LocalAccountReference`
- optional `ProviderSession`
- `SignupCompletion`

`SignupFailure` gives you:

- `SignupFailureReason`
- optional details
- optional `ProviderIdentity`

Typical failure reasons include:

- `DUPLICATE_LOCAL_ACCOUNT`
- `DUPLICATE_PROVIDER_IDENTITY`
- `PROVIDER_UNAVAILABLE`
- `PROVIDER_REJECTED`
- `UNSUPPORTED_PROVIDER`
- `INVALID_INPUT`
- `RECONCILIATION_REQUIRED`

### Signin results

`SigninResult` is sealed:

- `SigninSuccess`
- `SigninFailure`

`SigninSuccess` gives you:

- `ProviderSession`
- `ProviderIdentity`
- optional `LocalAccountReference`

`SigninFailure` gives you:

- `SigninFailureReason`
- optional details

Typical failure reasons include:

- `INVALID_CREDENTIALS`
- `LOCAL_ACCOUNT_NOT_LINKED`
- `LOCAL_ACCOUNT_DISABLED`
- `PROVIDER_UNAVAILABLE`
- `PROVIDER_REJECTED`
- `UNSUPPORTED_PROVIDER`
- `MALFORMED_COMMAND`

## Example Consumer Setup

This is the typical consumer shape:

```java
IdentityProviderPort provider = new SupabaseIdentityProviderPort(...);
LocalAccountLookupPort lookup = new AccountLookupAdapter(...);
LocalAccountCreationPort creation = new AccountCreationAdapter(...);
LocalAccountDuplicateCheckPort duplicates = new DuplicateCheckAdapter(...);

AuthenticationService auth = AuthenticationServiceFactory.create(
        AuthenticationConfiguration.of(
                provider,
                lookup,
                creation,
                duplicates,
                AuthenticationPolicy.defaults(),
                Clock.systemUTC()
        )
);
```

Then:

```java
SignupResult signup = auth.signup(signupCommand);
SigninResult signin = auth.signin(signinCommand);
```

Your application decides how to map those results to HTTP, CLI output, UI state, or further workflow steps.

## Package Guide

Use these packages from the outside:

- `de.gupta.security.janus.api`: main entrypoints and configuration
- `de.gupta.security.janus.api.command`: public signup/signin commands
- `de.gupta.security.janus.adapter.local`: ports for your local account store
- `de.gupta.security.janus.adapter.provider`: port for your identity provider
- `de.gupta.security.janus.domain.model.common`: shared result value objects
- `de.gupta.security.janus.domain.model.signup`: public signup result types
- `de.gupta.security.janus.domain.model.signin`: public signin result types
- `de.gupta.security.janus.domain.model.provider`: normalized provider-side result types
- `de.gupta.security.janus.domain.model.local`: normalized local-account creation result types

You normally do not need the internal application-service package.

## What Janus Does Not Assume

Janus does not assume:

- a specific persistence model
- a specific framework
- a specific token format
- a specific provider
- that signin must always return one JWT string

That is why provider results are normalized but still rich. A local provider, Supabase adapter, or another IdP can all return session data through the same Janus result flow.

## Current Scope

Janus is a core library today.

That means:

- you call it from your own controllers, handlers, or services
- you implement the ports in your application
- you map Janus results to your transport layer yourself

Spring support can be added later on top of this core without changing the fundamental flow.
