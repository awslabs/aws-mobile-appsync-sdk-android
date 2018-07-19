# Change Log - AWS AppSync SDK for Android

## [Release 2.6.22](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.22)

### New Features

* Adds support for AWS AppSync Defined Scalars such as `AWSTimestamp`.

## [Release 2.6.21](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.21)

### Enhancements

* Call `onCompleted` method of `AppSyncSubscriptionCall.Callback` when a subscription is disconnected.
* Remove static references to context. See [issue #13](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/13)

## [Release 2.6.20](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.20)

### Bug Fixes

* Prevent crashing when retrieving credentials to sign AppSync requests. Errors will now be routed to the `onError` callback. See [issue #16](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/16)
* Remove references to subscription callback when subscription is cancelled. See [issue #13](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/13)

### Enhancements

* Lazy load token in `BasicCognitoUserPoolsAuthProvider`.

## [Release 2.6.19](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.19)

### Enhancements

* Improve dependency injection for gradle plugin.
Uses `implementation` instead of `compile` when using higher than gradle version 2.
Note: gradle version 0 and 1 are note supported.
* Improve `BasicCognitoUserPoolsAuthProvider` retrieval of token.

### Bug Fixes

* Fixed dependency `com.moowork.gradle:gradle-node-plugin:1.0.0` in gradle plugin

## [Release 2.6.18](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.18)

### New Features

* Adds OpenID Connect (OIDC) support as an authorization option.

## [Release 2.6.17](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.17)

### Enhancements

* Improve synchronization of shared data structures in multiple subscriptions.
* Fixed bug that caused sigv4 signing not to be attached when okhttp client was specified in builder. See [PR #4](https://github.com/awslabs/aws-mobile-appsync-sdk-android/pull/4)

## [Release 2.6.16](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.16)

### New Features

* Subscription support.
* Complex objects allow fields to be S3 objects.
* Conflict resolution surfaces mutation conflicts so that they can be resolved through a callback.

## [Release 2.6.15](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.15)

### New Features

* Initial release with support for Cognito UserPools, Cognito Identity, and API key based authentication.
* Optimistic updates allow the cache to be updated before a server response is received (i.e. slow network or offline)
* Offline mutation allows mutations to be queued while client is offline until online again.
