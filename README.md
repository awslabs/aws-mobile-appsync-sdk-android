## App Sync SDK for Android
[![GitHub release](https://img.shields.io/github/release/awslabs/aws-mobile-appsync-sdk-android.svg)](https://github.com/awslabs/aws-mobile-appsync-sdk-android/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.amazonaws/aws-android-sdk-appsync-pom.svg)]()

Android library files for Offline, Sync, Sigv4.

## Samples

1. A sample app using the events sample schema can be found here: https://github.com/aws-samples/aws-mobile-appsync-events-starter-android

2. A step by step walkthrough of a posts app can be found here: https://docs.aws.amazon.com/appsync/latest/devguide/building-a-client-app-android.html

## Setup

### Gradle setup

#### Project's build.gradle

In the project's `build.gradle`, add the following dependency in
the build script:

```
    classpath 'com.amazonaws:aws-android-sdk-appsync-gradle-plugin:2.6.16'
```

**Sample project's build.gradle**

```
    // Top-level build file where you can add configuration options common to all sub-projects/modules.
    buildscript {
        // ..other code..
        dependencies {
            classpath 'com.android.tools.build:gradle:3.0.1'
            classpath 'com.amazonaws:aws-android-sdk-appsync-gradle-plugin:2.6.16'
            // NOTE: Do not place your application dependencies here; they belong
            // in the individual module build.gradle files
        }
    }
```

#### App's build.gradle

In the app's `build.gradle`, add the following plugin:

```
    apply plugin: 'com.amazonaws.appsync'
```

Add the following dependency:

```
    compile 'com.amazonaws:aws-android-sdk-appsync:2.6.16'
```

**Sample app's build.gradle**

```
    apply plugin: 'com.android.application'
    apply plugin: 'com.amazonaws.appsync'
    android {
        // Typical items
    }
    dependencies {
        // Typical dependencies
        compile 'com.amazonaws:aws-android-sdk-appsync:2.6.16'
    }
```

### App's AndroidManifest.xml

Add the permissions to access network state to determine if the device
is offline.

```
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

```
AWSAppSyncClient client = AWSAppSyncClient.builder()
                    .context(context)
                    .apiKey(new BasicAPIKeyAuthProvider(Constants.APPSYNC_API_KEY)) // API Key based authorization
                    .region(Constants.APPSYNC_REGION)
                    .serverUrl(Constants.APPSYNC_API_URL)
                    .build();
```

## Make a call

```
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
