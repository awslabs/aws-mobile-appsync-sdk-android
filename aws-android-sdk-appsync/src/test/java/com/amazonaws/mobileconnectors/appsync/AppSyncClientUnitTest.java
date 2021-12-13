package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.content.res.Resources;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.AWSLambdaAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.OidcAuthProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.ApolloLogger;
import com.apollographql.apollo.internal.RealAppSyncCall;
import com.apollographql.apollo.internal.RealAppSyncSubscriptionCall;
import com.apollographql.apollo.internal.subscription.SubscriptionManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class AppSyncClientUnitTest {

    Resources res;
    Context mockContext;
    Context shadowContext;
    ApolloLogger mockLogger;
    Subscription<?, ?, ?> mockSubscription;
    SubscriptionManager mockSubscriptionManager;
    ApolloClient mockApolloClient;
    RealAppSyncCall<?> mockSubscriptionMetadataRequest;

    private static final String TAG = AppSyncClientUnitTest.class.getSimpleName();

    private String jsonConfig;
    private AWSConfiguration awsConfiguration;

    @Before
    public void setup() {
        shadowContext = RuntimeEnvironment.application;
        mockContext = Mockito.mock(Context.class);
        mockLogger = Mockito.mock(ApolloLogger.class);
        mockSubscription = Mockito.mock(Subscription.class);
        mockSubscriptionManager = Mockito.mock(SubscriptionManager.class);
        mockApolloClient = Mockito.mock(ApolloClient.class);
        mockSubscriptionMetadataRequest  = Mockito.mock(RealAppSyncCall.class);
        res = Mockito.mock(Resources.class);
        Mockito.when(mockContext.getResources()).thenReturn(res);
        jsonConfig = "{\n" +
                "  \"CredentialsProvider\": {\n" +
                "    \"CognitoIdentity\": {\n" +
                "      \"AwsIam\": {\n" +
                "        \"PoolId\": \"us-east-1:xxxx\",\n" +
                "        \"Region\": \"us-east-1\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"CognitoUserPool\": {\n" +
                "    \"AmazonCognitoUserPools\": {\n" +
                "      \"PoolId\": \"us-east-1_zzzz\",\n" +
                "      \"AppClientId\": \"aaaa\",\n" +
                "      \"AppClientSecret\": \"bbbb\",\n" +
                "      \"Region\": \"us-east-1\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"AppSync\": {\n" +
                "    \"Default\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"ApiKey\": \"da2-xxxx\",\n" +
                "      \"AuthMode\": \"API_KEY\"\n" +
                "    },\n" +
                "    \"ApiKey\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"ApiKey\": \"da2-xxxx\",\n" +
                "      \"AuthMode\": \"API_KEY\",\n" +
                "      \"ClientDatabasePrefix\": \"prefix_from_config\"\n" +
                "    },\n" +
                "    \"AwsIam\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"AuthMode\": \"AWS_IAM\"\n" +
                "    },\n" +
                "    \"AmazonCognitoUserPools\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"AuthMode\": \"AMAZON_COGNITO_USER_POOLS\"\n" +
                "    },\n" +
                "    \"OpenidConnect\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"AuthMode\": \"OPENID_CONNECT\"\n" +
                "    },\n" +
                "    \"Lambda\": {\n" +
                "      \"ApiUrl\": \"https://xxxx.appsync-api.us-east-1.amazonaws.com/graphql\",\n" +
                "      \"Region\": \"us-east-1\",\n" +
                "      \"AuthMode\": \"AWS_LAMBDA\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        Mockito.when(res.openRawResource(3213210)).thenReturn(new ByteArrayInputStream(jsonConfig.getBytes()));
        awsConfiguration = new AWSConfiguration(mockContext, 3213210);
    }

    @Test
    public void testDefault() {
        awsConfiguration.setConfiguration("Default");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testClientDatabasePrefixFromBuilder() {
        String prefix = "prefix_from_builder";
        awsConfiguration.setConfiguration("Default");
        // Create an instance with useClientDatabasePrefix = true
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                                                                  .context(shadowContext)
                                                                  .clientDatabasePrefix(prefix)
                                                                  .useClientDatabasePrefix(true)
                                                                  .awsConfiguration(awsConfiguration)
                                                                  .build();

        // Create an instance with useClientDatabasePrefix = false
        final AWSAppSyncClient awsAppSyncClientWithoutPrefix = AWSAppSyncClient.builder()
                                                                  .context(shadowContext)
                                                                  .clientDatabasePrefix(prefix)
                                                                  .useClientDatabasePrefix(false)
                                                                  .awsConfiguration(awsConfiguration)
                                                                  .build();
        // Verify that prefix is set.
        assertNotNull(awsAppSyncClient);
        assertEquals(prefix, awsAppSyncClient.clientDatabasePrefix);

        // Verify prefix is not set.
        assertNotNull(awsAppSyncClientWithoutPrefix);
        assertNull(awsAppSyncClientWithoutPrefix.clientDatabasePrefix);
    }

    @Test
    public void testClientDatabasePrefixFromConfig() throws JSONException {
        awsConfiguration.setConfiguration("ApiKey");
        JSONObject appSyncJsonObject = awsConfiguration.optJsonObject("AppSync");
        String prefix = appSyncJsonObject.getString("ClientDatabasePrefix");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                                                                  .context(shadowContext)
                                                                  .useClientDatabasePrefix(true)
                                                                  .awsConfiguration(awsConfiguration)
                                                                  .build();
        assertNotNull(awsAppSyncClient);
        assertEquals(prefix, awsAppSyncClient.clientDatabasePrefix);
    }

    @Test
    public void testApiKeyAuthProvider() {
        awsConfiguration.setConfiguration("ApiKey");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .apiKey(new BasicAPIKeyAuthProvider(awsConfiguration))
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testAwsIamAuthProvider() {
        awsConfiguration.setConfiguration("AwsIam");
        final CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(awsConfiguration);
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .credentialsProvider(credentialsProvider)
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testAmazonCognitoUserPoolsAuthProvider() {
        awsConfiguration.setConfiguration("AmazonCognitoUserPools");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(shadowContext, awsConfiguration);
        BasicCognitoUserPoolsAuthProvider basicCognitoUserPoolsAuthProvider = new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .cognitoUserPoolsAuthProvider(basicCognitoUserPoolsAuthProvider)
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testOpenidConnectAuthProvider() {
        awsConfiguration.setConfiguration("OpenidConnect");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .oidcAuthProvider(new OidcAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        return null;
                    }
                })
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testAWSLambdaAuthProvider() {
        awsConfiguration.setConfiguration("Lambda");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .awsLamdbaAuthProvider(new AWSLambdaAuthProvider() {
                    @Override
                    public String getLatestAuthToken() {
                        return "AWS_LAMBDA_AUTHORIZATION_TOKEN";
                    }
                })
                .build();
        assertNotNull(awsAppSyncClient);
    }

    @Test
    public void testRealAppSyncSubscriptionCallErrorHandling() throws InterruptedException {
        RealAppSyncSubscriptionCall<Object> call = new RealAppSyncSubscriptionCall(mockSubscription, mockSubscriptionManager, mockApolloClient, mockLogger, mockSubscriptionMetadataRequest);
        call.cancel();
        Mockito.timeout(500);
        final CountDownLatch waitForCall = new CountDownLatch(1);
        call.execute(new AppSyncSubscriptionCall.Callback<Object>() {

            @Override
            public void onResponse(@Nonnull Response<Object> response) {
                fail("Execute should not succeed in canceled state");
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                assertEquals(ApolloCanceledException.class, e.getClass());
                waitForCall.countDown();
            }

            @Override
            public void onCompleted() {
                fail("Execute should not complete in canceled state");
            }
        });

        assertTrue(waitForCall.await(100, TimeUnit.MILLISECONDS));
    }

    @Test(expected = RuntimeException.class)
    public void testConfigMismatch_ApiKey() {
        awsConfiguration.setConfiguration("AwsIam");
        final CognitoCredentialsProvider credentialsProvider = new CognitoCredentialsProvider(awsConfiguration);

        awsConfiguration.setConfiguration("ApiKey");
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Test(expected = RuntimeException.class)
    public void testConfigMismatch_AmazonCognitoUserPools() {
        awsConfiguration.setConfiguration("ApiKey");
        APIKeyAuthProvider apiKeyAuthProvider = new BasicAPIKeyAuthProvider(awsConfiguration);

        awsConfiguration.setConfiguration("AmazonCognitoUserPools");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(shadowContext, awsConfiguration);
        BasicCognitoUserPoolsAuthProvider basicCognitoUserPoolsAuthProvider = new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .apiKey(apiKeyAuthProvider)
                .build();
    }

    @Test(expected = RuntimeException.class)
    public void testMultipleAuth() {
        awsConfiguration.setConfiguration("ApiKey");
        APIKeyAuthProvider apiKeyAuthProvider = new BasicAPIKeyAuthProvider(awsConfiguration);

        awsConfiguration.setConfiguration("AmazonCognitoUserPools");
        CognitoUserPool cognitoUserPool = new CognitoUserPool(shadowContext, awsConfiguration);
        BasicCognitoUserPoolsAuthProvider basicCognitoUserPoolsAuthProvider = new BasicCognitoUserPoolsAuthProvider(cognitoUserPool);
        final AWSAppSyncClient awsAppSyncClient = AWSAppSyncClient.builder()
                .context(shadowContext)
                .awsConfiguration(awsConfiguration)
                .apiKey(apiKeyAuthProvider)
                .cognitoUserPoolsAuthProvider(basicCognitoUserPoolsAuthProvider)
                .build();
    }
}
