package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.sigv4.APIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicAPIKeyAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amazonaws.mobileconnectors.appsync.sigv4.OidcAuthProvider;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.google.android.apps.common.testing.accessibility.framework.proto.FrameworkProtos;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    private static final String TAG = AppSyncClientUnitTest.class.getSimpleName();

    private String jsonConfig;
    private AWSConfiguration awsConfiguration;

    @Before
    public void setup() {
        shadowContext = ShadowApplication.getInstance().getApplicationContext();
        mockContext = Mockito.mock(Context.class);
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
                "      \"AuthMode\": \"API_KEY\"\n" +
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
