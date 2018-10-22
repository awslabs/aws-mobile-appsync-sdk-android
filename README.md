## AWS AppSync SDK for Android
[![GitHub release](https://img.shields.io/github/release/awslabs/aws-mobile-appsync-sdk-android.svg)](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.amazonaws/aws-android-sdk-appsync.svg)]()
[![Build Status](https://travis-ci.org/awslabs/aws-mobile-appsync-sdk-android.svg?branch=master)](https://travis-ci.org/awslabs/aws-mobile-appsync-sdk-android)

The AWS AppSync SDK for Android enables you to access your AWS AppSync backend and perform operations like `Queries`, `Mutations`, and `Subscriptions`. The SDK also includes support for offline operations. This SDK is based off of the Apollo project found [here](https://github.com/apollographql/apollo-android). Please log questions for this client SDK in this repo and questions for the AppSync service in the [official AWS AppSync forum](https://forums.aws.amazon.com/forum.jspa?forumID=280&start=0).

## Samples

1. A sample app using the events sample schema can be found here: https://github.com/aws-samples/aws-mobile-appsync-events-starter-android

2. A step by step walkthrough of a posts app can be found here: https://docs.aws.amazon.com/appsync/latest/devguide/building-a-client-app-android.html

## Setup

### Gradle setup

#### Project's build.gradle

In the project's `build.gradle`, add the following dependency in
the build script:

```groovy
    classpath 'com.amazonaws:aws-android-sdk-appsync-gradle-plugin:2.6.+'
```

**Sample project's build.gradle**

```groovy
    // Top-level build file where you can add configuration options common to all sub-projects/modules.
    buildscript {
        // ..other code..
        dependencies {
            classpath 'com.android.tools.build:gradle:3.0.1'
            classpath 'com.amazonaws:aws-android-sdk-appsync-gradle-plugin:2.6.+'
            // NOTE: Do not place your application dependencies here; they belong
            // in the individual module build.gradle files
        }
    }
```

#### App's build.gradle

In the app's `build.gradle`, add the following plugin:

```groovy
    apply plugin: 'com.amazonaws.appsync'
```

Add the following dependency:

```groovy
    compile 'com.amazonaws:aws-android-sdk-appsync:2.6.+'
```

**Sample app's build.gradle**

```groovy
    apply plugin: 'com.android.application'
    apply plugin: 'com.amazonaws.appsync'
    android {
        // Typical items
    }
    dependencies {
        // Typical dependencies
        compile 'com.amazonaws:aws-android-sdk-appsync:2.6.+'
    }
```

### App's AndroidManifest.xml

Add the permissions to access network state to determine if the device
is offline.

```xml
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Code generation

To interact with AppSync, your client needs to define GraphQL queries and mutations.

For example, create a file named `./app/src/main/graphql/com/amazonaws/demo/posts/posts.graphql`:

```
    query GetPost($id:ID!) {
     getPost(id:$id) {
         id
         title
         author
         content
         url
         version
     }
    }

    mutation AddPost($id: ID!, $author: String!, $title: String, $content: String, $url: String, $ups: Int!, $downs: Int!, $expectedVersion: Int!) {
      putPost(id: $id, author: $author, title: $title, content: $content, url: $url, ups: $ups, downs: $downs, version: $expectedVersion) {
        id
        title
        author
        url
        content
      }
    }
```

Next, fetch the ``schema.json`` file from the AppSync console and place it alongside the `posts.graphql` file.
``./app/src/main/graphql/com/amazonaws/demo/posts/schema.json``

Now build the project and the generated source files will be available to
use within the app. They will not show up in your source directory, but
are added in the build path.

## Create a client

### Configuration via code

```java
AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .apiKey(new BasicAPIKeyAuthProvider(Constants.APPSYNC_API_KEY)) // API Key based authorization
                    .region(Constants.APPSYNC_REGION)
                    .serverUrl(Constants.APPSYNC_API_URL)
                    .build();
```

### Configuration via a config file

Alternatively, you can use the `awsconfiguration.json` file to supply the configuration information required to create a `AWSAppSyncClient` object.

Create a file named `awsconfiguration.json` under `res/raw` directory of your app.

```
{
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "ApiKey": "YOUR-API-KEY",
            "AuthMode": "API_KEY"
        }
    }
}
```

The `AWSConfiguration` represents the configuration information present in `awsconfiguration.json` file. By default, the information under `Default` section will be used. 

```java
AWSConfiguration awsConfig = new AWSConfiguration(context);

AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .build();
```

You can override the `Default` configuration by using the `AWSConfiguration#setConfiguration()` method.

```
{
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "ApiKey": "YOUR-API-KEY",
            "AuthMode": "API_KEY"
        },
        "Custom": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-2",
            "ApiKey": "YOUR-API-KEY",
            "AuthMode": "API_KEY"
        }
   }
}
```

```java
AWSConfiguration awsConfig = new AWSConfiguration(context);
awsConfig.setConfiguration("Custom");

AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .build();
```

## Authentication Modes

When making calls to AWS AppSync, there are several ways to authenticate those calls. The API key authorization (**API_KEY**) is the simplest way to onboard, but we recommend you use either Amazon IAM (**AWS_IAM**) or Amazon Cognito UserPools (**AMAZON\_COGNITO\_USER_POOLS**) or any OpenID Connect Provider (**OPENID_CONNECT**) after you onboard with an API key.

### API Key

For authorization using the API key, update the `awsconfiguration.json` file and code snippet as follows:

#### Configuration

Add the following snippet to your `awsconfiguration.json` file.

```
{
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "ApiKey": "YOUR-API-KEY",
            "AuthMode": "API_KEY"
        }
   }
}
```

#### Code

Add the following code to use the information in the `Default` section from `awsconfiguration.json` file.


```java
AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(new AWSConfiguration(context))
                    .build();
```

### AWS IAM

For authorization using the Amazon IAM credentials using Amazon IAM or Amazon STS or Amazon Cognito, update the `awsconfiguration.json` file and code snippet as follows:

#### Configuration

Add the following snippet to your `awsconfiguration.json` file.

```
{
    "CredentialsProvider": {
        "CognitoIdentity": {
            "Default": {
                "PoolId": "YOUR-COGNITO-IDENTITY-POOLID",
                "Region": "us-east-1"
            }
        }
    },
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "AuthMode": "AWS_IAM"
       }
   }
}
```

#### Code

Add the following code to use the information in the `Default` section from `awsconfiguration.json` file.

```java
AWSConfiguration awsConfig = new AWSConfiguration(context);

CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(context, awsConfig);

AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .credentialsProvider(credentialsProvider)
                    .build();
```


### Amazon Cognito UserPools

For authorization using the Amazon Cognito UserPools, update the `awsconfiguration.json` file and code snippet as follows:

#### Configuration

Add the following snippet to your `awsconfiguration.json` file.

```
{
    "CognitoUserPool": {
        "Default": {
            "PoolId": "POOL-ID",
            "AppClientId": "APP-CLIENT-ID",
            "AppClientSecret": "APP-CLIENT-SECRET",
            "Region": "us-east-1"
        }
    },
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "AuthMode": "AMAZON_COGNITO_USER_POOLS"
        }
   }
}
```

#### Code

Add the following dependency to your app in order to use Amazon Cognito UserPools:

```
dependencies {
    implementation 'com.amazonaws:aws-android-sdk-cognitoidentityprovider:2.6.+'
}
```

Add the following code to use the information in the `Default` section from `awsconfiguration.json` file.


```java
AWSConfiguration awsConfig = new AWSConfiguration(context);

CognitoUserPool cognitoUserPool = new CognitoUserPool(context, awsConfig);
BasicCognitoUserPoolsAuthProvider basicCognitoUserPoolsAuthProvider = new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);

AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(awsConfig)
                    .cognitoUserPoolsAuthProvider(basicCognitoUserPoolsAuthProvider)
                    .build();
```

### OIDC (OpenID Connect)

For authorization using any OIDC (OpenID Connect) Identity Provider, update the `awsconfiguration.json` file and code snippet as follows:

#### Configuration

Add the following snippet to your `awsconfiguration.json` file.

```
{
    "AppSync": {
        "Default": {
            "ApiUrl": "YOUR-GRAPHQL-ENDPOINT",
            "Region": "us-east-1",
            "AuthMode": "OPENID_CONNECT"
        }
   }
}
```

#### Code

Add the following code to use the information in the `Default` section from `awsconfiguration.json` file.

```java
AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .awsConfiguration(new AWSConfiguration(context))
                    .oidcAuthProvider(new OidcAuthProvider() {
                        @Override
                        public String getLatestAuthToken() {
                            return "jwt-token-from-oidc-provider";
                        }
                    })
                    .build();
```

## Make a call

```java
public void addPost() {
    AddPostMutation addPostMutation = AddPostMutation.builder()
            .id(UUID.randomUUID().toString())
            .title(title)
            .author(author)
            .url(url)
            .content(content)
            .ups(0)
            .downs(0)
            .expectedVersion(1)
            .build();
    client.mutate(addPostMutation).enqueue(postsCallback);
}

private GraphQLCall.Callback<AddPostMutation.Data> postsCallback = new GraphQLCall.Callback<AddPostMutation.Data>() {
    @Override
    public void onResponse(@Nonnull final Response<AddPostMutation.Data> response) {
        // non-UI calls

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // UI related calls
            }
        });
    }

    @Override
    public void onFailure(@Nonnull final ApolloException e) {
        // Error handling
    }
};
```

## License

This library is licensed under the Amazon Software License.
