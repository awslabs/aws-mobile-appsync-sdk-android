# Change Log - AWS AppSync SDK for Android

## [Release 2.7.1](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.7.0)

### Misc. Updates
* `AWSAppSync` now depends on `AWSCore` version `2.9.1` instead of `2.8.2`.

### Bug Fixes
* Adjusted mutation processing logic to remove mutations from the queue only after they have been processed by the server and maintain sequential execution semantics. See [issue #40](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/40), [issue #33](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/33), [issue #72](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/72), and [issue #82](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/82)


## [Release 2.7.0](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.7.0)

### New Features
* Added support for Delta Sync Feature
    Delta Sync allows you to perform automatic synchronization with an AWS AppSync GraphQL server. The client will perform reconnection, exponential backoff, and retries when network errors take place for simplified data replication to devices. For more details, please refer [documentation.](https://aws-amplify.github.io/docs/android/api)

### Enhancements
* Added reconnection logic to subscriptions. Subscriptions will be automatically reconnected if the device loses connectivity temporarily or transitions between networks. See [issue #45](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/45)

### Bug Fixes
* Fixed bug in connection logic to close MQTT connection if all subscriptions are cancelled. See [issue #7](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/7)


## [Release 2.6.28](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.28)

### Misc. Updates
* `AWSAppSync` now depends on `AWSCore` version `2.8.0` instead of `2.7.7`.

## [Release 2.6.27](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.27)

### Bug Fixes
* Fixed bug in subscribe call that was setting the QoS to be 0. It is now correctly set to QoS 1. See [issue #54](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/54)
* Fixed bug that was causing a ConcurrentModificationException in the logic to handle connection loss. See [PR #41](https://github.com/awslabs/aws-mobile-appsync-sdk-android/pull/41) and [issue #53](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/53)
* Fixed null pointer exception in the AppSyncOfflineMutationInterceptor. See [issue #51](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/51)

## [Release 2.6.26](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.26)

### New Features
* Allow multiple active subscriptions while using API_KEY auth.

### Enhancements
* Adjusted logic for service call retries. Retries will have adjusted Jitter logic and calls will be retried until max exponential backoff time has been reached.

### Bug Fixes
* Fixed connection handling bug that was causing unexpected disconnects. See [issue #22](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/22)

## [Release 2.6.25](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.25)

### New Features
* Add support for `AWSConfiguration` through `awsconfiguration.json` in `AWSAppSyncClient`.
 
## [Release 2.6.24](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.24)

### Bug Fixes
* Fix a bug where multiple instances of same subscriptions when cancelled cause a `ConcurrentModificationException`. See [issue #22](https://github.com/awslabs/aws-mobile-appsync-sdk-android/issues/22)

## [Release 2.6.23](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.23)

### Enhancements
* Add retries for service calls with error code 5XX and 429. GraphQL calls for mutation, query, and subscription will be retried. This does not include retrying the connection for subscription messages.

## [Release 2.6.22](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases/tag/release_v2.6.22)

### New Features

* Adds support for AWS AppSync Defined Scalars such as `AWSTimestamp`.

### Bug Fixes

* Fix premature execution of `onCompleted` method of `AppSyncSubscriptionCall.Callback`.

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
