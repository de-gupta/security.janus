# security.janus

`security.janus` is an authentication orchestration library for signup and signin.

It is intentionally narrow:

- no persistence implementation
- no refresh-token flow
- no password reset, MFA, logout, or email verification
- no HTTP controllers or transport mapping

You bring your own local account store and your own identity provider implementation. Janus coordinates the flow and returns rich, typed results.

## Modules

Janus currently ships as two modules:

- `security.janus.core`: framework-agnostic orchestration and public auth models
- `security.janus.spring`: Spring / Spring Boot wiring on top of the core module

Use the core module directly if you want full manual assembly. Use the Spring module if you want Janus assembled from
Spring beans.

## Quick Start

### Core only

Create an `AuthenticationService` yourself:

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

Then call:

```java
SignupResult signupResult = service.signup(signupCommand);
SigninResult signinResult = service.signin(signinCommand);
```

### Spring / Spring Boot

Add `security.janus.spring` and provide the required beans.

If you use Spring Boot, Janus auto-configures an `AuthenticationService` bean when the required collaborators are
present.

If you use plain Spring, import:

```java
@Import(JanusAuthenticationSpringConfiguration.class)
```

You then inject:

```java
private final AuthenticationService authenticationService;
```

## Core Module

### What Consumers Provide

For the core module, consumers provide a few narrow ports.

In practice, that means:

- `IdentityProviderPort`
- `LocalAccountLookupPort`
- `LocalAccountCreationPort`
- optionally `LocalAccountDuplicateCheckPort`

Consumers also need a concrete `LocalAccountIdentityView` implementation for successful local-account lookup results.

#### Provider adapter

Implement `IdentityProviderPort` when Janus should talk to an auth provider such as:

- local auth
- Supabase
- Auth0
- another IdP

Janus calls:

- `signup(SignupProviderCommand)`
- `signin(SigninProviderCommand)`

Your adapter returns normalized provider results from `de.gupta.security.janus.core.domain.model.provider`, not raw
vendor DTOs.

#### Local account adapters

Implement the local account ports for your own application model:

- `LocalAccountLookupPort`
- `LocalAccountCreationPort`
- optionally `LocalAccountDuplicateCheckPort`

These ports let Janus:

- check whether a local account already exists
- create a local account after provider signup succeeds
- resolve a local account from provider subject data during signin

For the lookup path, consumers must return an implementation of `LocalAccountIdentityView` when a linked local account
is found.

So on the local-account side, the full consumer responsibility is:

- implement `LocalAccountLookupPort`
- implement `LocalAccountCreationPort`
- optionally implement `LocalAccountDuplicateCheckPort`
- provide a concrete `LocalAccountIdentityView` implementation for lookup results

Janus does not require your main user/entity model to implement a large shared interface. A common approach is to create
a small adapter record or projection class that implements `LocalAccountIdentityView`.

### What Consumers Get

The core module gives you a configured `AuthenticationService`.

Both main outcomes are sealed result models:

- `SignupResult`
- `SigninResult`

Janus does not collapse normal control flow into booleans or exceptions.

### Signup Flow

Default behavior is provider-first:

1. validate the incoming signup command
2. optionally check for obvious local duplicates
3. call `IdentityProviderPort.signup(...)`
4. if provider signup succeeds, call `LocalAccountCreationPort`
5. return a typed `SignupResult`

If provider signup succeeds but local account creation fails, Janus returns `SignupFailure` with reason `RECONCILIATION_REQUIRED` and includes the created `ProviderIdentity` so your application can repair or compensate deliberately.

### Signin Flow

Signin is provider-driven:

1. validate the incoming signin command
2. call `IdentityProviderPort.signin(...)`
3. if provider signin succeeds, resolve the linked local account through `LocalAccountLookupPort`
4. return a typed `SigninResult`

By default, Janus expects a linked local account. If you want provider-authenticated but unlinked signin to succeed, use `AuthenticationPolicy.allowingUnlinkedLocalSignin()`.

### Commands You Send In

Public commands live in `de.gupta.security.janus.core.api.command`.

#### Signup

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

#### Signin

Use `SigninCommand`.

```java
SigninCommand command = SigninCommand.of(
        "alice@example.com",
        "secret",
        "supabase"
);
```

### Results You Get Back

#### Signup results

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

#### Signin results

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

### Core Consumer Setup

Typical core setup:

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

### Core Convenience Factories

Core also supports adapting JDK functional types into Janus ports:

-

`IdentityProviderPort.of(Function<SignupProviderCommand, ProviderSignupResult>, Function<SigninProviderCommand, ProviderSigninResult>)`

- `LocalAccountLookupPort.of(BiFunction<String, String, Optional<LocalAccountIdentityView>>)`
- `LocalAccountLookupPort.of(Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>)`
- `LocalAccountCreationPort.of(Function<LocalAccountCreationCommand, LocalAccountCreationResult>)`
- `LocalAccountDuplicateCheckPort.of(Predicate<String>)`

These are especially useful for Spring bean wiring, but they also work outside Spring.

## Spring Module

### What Consumers Provide

The Spring module assembles Janus from Spring beans.

Required collaborators:

- `IdentityProviderPort` or both provider functions
- `LocalAccountLookupPort` or one supported lookup function
- `LocalAccountCreationPort` or one creation function

Optional collaborators:

- `LocalAccountDuplicateCheckPort` or `Predicate<String>`
- `AuthenticationPolicy`
- `Clock`

If `AuthenticationPolicy` is absent, Janus uses `AuthenticationPolicy.defaults()`.

If `Clock` is absent, Janus uses `Clock.systemUTC()`.

If duplicate checking is absent, signup simply skips the duplicate pre-check.

If your lookup path can return a linked local account, you also need a concrete `LocalAccountIdentityView`
implementation for that returned value.

So the practical rule is:

- required collaborators are `IdentityProviderPort`, `LocalAccountLookupPort`, and `LocalAccountCreationPort`
- `LocalAccountDuplicateCheckPort` is optional
- `LocalAccountIdentityView` is not a standalone Spring bean Janus looks up, but it is a consumer-implemented projection
  type used by lookup results

### What Consumers Get

The Spring module gives you an `AuthenticationService` Spring bean.

That bean is exactly the same public Janus core service you would build manually yourself. Spring only performs assembly
and dependency resolution around it.

### Supported Spring Wiring Styles

Spring supports two styles.

#### 1. Explicit Janus port beans

This is the most explicit and most stable option.

```java

@Bean
IdentityProviderPort identityProviderPort()
{ ...}

@Bean
LocalAccountLookupPort localAccountLookupPort()
{ ...}

@Bean
LocalAccountCreationPort localAccountCreationPort()
{ ...}

@Bean
LocalAccountDuplicateCheckPort localAccountDuplicateCheckPort()
{ ...}
```

If `localAccountLookupPort()` finds an account, it must return an object that implements `LocalAccountIdentityView`.

#### 2. JDK functional beans

This is the lighter-weight option when you do not want to write port wrapper classes.

Supported fallback bean shapes:

- `Function<SignupProviderCommand, ProviderSignupResult>`
- `Function<SigninProviderCommand, ProviderSigninResult>`
- `BiFunction<String, String, Optional<LocalAccountIdentityView>>`
- `Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>`
- `Function<LocalAccountCreationCommand, LocalAccountCreationResult>`
- `Predicate<String>`

For local account lookup, the simplest happy path is usually:

```java

@Bean
BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookup()
{ ...}
```

The two string inputs are:

- provider
- external subject

The returned value still uses `LocalAccountIdentityView`, so consumers need a small implementation of that interface
somewhere in their application.

### Resolution Rules

Spring resolves collaborators in this order:

1. explicit Janus port bean
2. supported function bean fallback
3. default, if the dependency is optional

Important rules:

- explicit Janus port beans win over function fallbacks
- if multiple beans match the same collaborator role, startup fails clearly
- for provider fallback, both signup and signin functions must be present together
- for lookup fallback, provide exactly one of:
  - `BiFunction<String, String, Optional<LocalAccountIdentityView>>`
  - `Function<LocalAccountLookupQuery, Optional<LocalAccountIdentityView>>`

### Spring Boot Usage

With Spring Boot, Janus auto-configures itself when the required collaborators are present.

Typical Boot setup with explicit ports:

```java

@Configuration
class JanusConsumerConfiguration
{
  @Bean
  IdentityProviderPort identityProviderPort()
  {
    return new SupabaseIdentityProviderAdapter(...);
  }

  @Bean
  LocalAccountLookupPort localAccountLookupPort()
  {
    return (provider, externalSubject) -> accountRepository
            .findByProviderAndExternalSubject(provider, externalSubject)
            .map(AccountIdentityView::new);
  }

  @Bean
  LocalAccountCreationPort localAccountCreationPort()
  {
    return command -> createLocalAccount(command);
  }

  @Bean
  LocalAccountDuplicateCheckPort localAccountDuplicateCheckPort()
  {
    return loginIdentifier -> accountRepository.existsByEmail(loginIdentifier);
  }
}
```

Then inject:

```java

@Service
class AuthenticationUseCase
{
  private final AuthenticationService authenticationService;

  AuthenticationUseCase(final AuthenticationService authenticationService)
  {
    this.authenticationService = authenticationService;
  }
}
```

### Spring Boot Usage With Functions

If you prefer lambda-style wiring:

```java

@Configuration
class JanusConsumerConfiguration
{
  @Bean
  Function<SignupProviderCommand, ProviderSignupResult> signupProvider()
  {
    return command -> providerSignup(command);
  }

  @Bean
  Function<SigninProviderCommand, ProviderSigninResult> signinProvider()
  {
    return command -> providerSignin(command);
  }

  @Bean
  BiFunction<String, String, Optional<LocalAccountIdentityView>> localAccountLookup()
  {
    return (provider, externalSubject) -> accountRepository
            .findByProviderAndExternalSubject(provider, externalSubject)
            .map(AccountIdentityView::new);
  }

  @Bean
  Function<LocalAccountCreationCommand, LocalAccountCreationResult> localAccountCreation()
  {
    return command -> createLocalAccount(command);
  }

  @Bean
  Predicate<String> localDuplicateCheck()
  {
    return loginIdentifier -> accountRepository.existsByEmail(loginIdentifier);
  }
}
```

### Plain Spring Usage

If you are not using Spring Boot, import the manual configuration:

```java

@Configuration
@Import(JanusAuthenticationSpringConfiguration.class)
class JanusConsumerConfiguration
{
}
```

After that, define the same required beans as above and inject `AuthenticationService`.

### Spring Public Entry Points

The Spring module intentionally exposes only a small public surface:

- `de.gupta.security.janus.spring.configuration.JanusAuthenticationSpringConfiguration`
- `de.gupta.security.janus.spring.configuration.JanusAuthenticationAutoConfiguration`

Everything else in the Spring module is internal support for bean resolution and assembly.

## Package Guide

Use these core packages from the outside:

- `de.gupta.security.janus.core.api`: main entrypoints and configuration
- `de.gupta.security.janus.core.api.command`: public signup/signin commands
- `de.gupta.security.janus.core.adapter.local`: local-account ports and lookup helpers
- `de.gupta.security.janus.core.adapter.provider`: identity-provider port and provider commands
- `de.gupta.security.janus.core.domain.model.common`: shared result value objects
- `de.gupta.security.janus.core.domain.model.signup`: public signup result types
- `de.gupta.security.janus.core.domain.model.signin`: public signin result types
- `de.gupta.security.janus.core.domain.model.provider`: normalized provider-side result types
- `de.gupta.security.janus.core.domain.model.local`: normalized local-account creation result types

Use this Spring package from the outside:

- `de.gupta.security.janus.spring.configuration`: public Spring entrypoints

You normally do not need the internal application / facade packages in core or the internal assembly packages in spring.

## What Janus Does Not Assume

Janus does not assume:

- a specific persistence model
- a specific provider
- a specific token format
- a specific HTTP framework
- that signin must always return one JWT string

The core module stays framework-agnostic. The Spring module is optional wiring on top of that core.