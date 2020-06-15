
# Running the Integration tests

Integration test for the SDK are in the `aws-android-sdk-appsync-tests` directory, which is an independent gradle project. The tests depend on certain artifacts being published to the local maven repository. In order to publish required artifacts to the local maven repository and run the tests, execute following commands from the project root:

```
./gradlew publishToMavenLocal
cd aws-android-sdk-appsync-tests/
./gradlew connectedAndroidTest
```

To run tests from Android Studio, run `./gradlew publishToMavenLocal` from project root, then open the `aws-android-sdk-appsync-tests/` directory in Android Studio, and run the tests through the UI.

## Integration testing infrastructure

When trying to replicate the integration testing infrastructure in an AWS account, you can start by [creating an Amplify CLI project](https://docs.amplify.aws/cli/start/workflows) with the following configuration:

- Include the Auth category with the default settings provided by the CLI.
- Include the Storage category and only allow authenticated access.
- Include the API category.
    - Use the `schema.graphql` file stored in `src/main/assets/schema.graphql`.
    - Configure API Key, IAM and Cognito User Pools as authorization providers.
- Copy the `src/main/assets/Mutation.updateArticle.req.vtl` to your Amplify project under `amplify/backend/api/<your api name/resolvers`.
- Deploy your project to an AWS account by running `amplify push`.
- After successful deployment you will need to manually edit the GraphQL schema via the AppSync AWS console. The generated `CreateArticleInput` is missing the `version: Int` field. The schema for that type should look like the following:
```graphql
    input CreateArticleInput {
        id: ID
        author: String!
        title: String
        pdf: S3ObjectInput
        image: S3ObjectInput
        version: Int # Add this line
        _version: Int
    }
``` 
- Create a user to be used by the [integration tests](https://github.com/awslabs/aws-mobile-appsync-sdk-android/blob/57040fe1c9e918d9ff7d8a5fde9d57fbc2dbca92/aws-android-sdk-appsync-tests/src/androidTest/java/com/amazonaws/mobileconnectors/appsync/identity/CustomCognitoUserPool.java#L39)
- Update the `src/main/res/raw/awsconfiguration.json` file with the configuration returned by the Amplify CLI deployment. 

__NOTES__ 
- The integration tests for this repo are still a work-in-progress.
- The `src/main/graphql/com/amazonaws/mobileconnectors/appsync/demo/schema.json` is used by the AppSync build tool to generate code. If something needs to be added/modified/removed from the generated code, this is where you can try to manipulate that.