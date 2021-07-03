package com.amazonaws.mobileconnectors.appsync;

import android.content.Context;

import com.amazonaws.mobileconnectors.appsync.util.subscriptions.EnumFieldSubscription;
import com.amazonaws.mobileconnectors.appsync.util.subscriptions.TestEnum;
import com.apollographql.apollo.api.Subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import okhttp3.WebSocket;

/**
 * tests for WebSocketConnectionManager
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class WebSocketConnectionManagerTest {

    Context mockContext;
    Subscription<?, ?, ?> testSubscription;
    AppSyncSubscriptionCall.Callback mockCallback;
    WebSocketConnectionManager webSocketConnectionManager;
    SubscriptionAuthorizer mockSubscriptionAuthorizer;
    WebSocket mockWebSocket;

    @Before
    public void beforeEachTest() {
        // set up mocks
        mockContext = Mockito.mock(Context.class);
        testSubscription = new EnumFieldSubscription(TestEnum.TEST_ENUM);
        mockCallback = Mockito.mock(AppSyncSubscriptionCall.Callback.class);
        mockWebSocket = Mockito.mock(WebSocket.class);
        mockSubscriptionAuthorizer = Mockito.mock(SubscriptionAuthorizer.class);
        try {
            Mockito.when(mockSubscriptionAuthorizer.getAuthorizationDetails(Mockito.anyBoolean(), Mockito.<Subscription>any())).thenReturn(null);
        } catch (JSONException e) {
            fail("This shouldn't happen.");
        }

        // set up webSocketConnectionManager
        webSocketConnectionManager = new WebSocketConnectionManager(mockContext,
                null,
                mockSubscriptionAuthorizer,
                null,
                null,
                true);

        // set webSocketConnectionManager's websocket to mockWebSocket
        try {
            Field reader = WebSocketConnectionManager.class.getDeclaredField("websocket");
            reader.setAccessible(true);
            reader.set(webSocketConnectionManager, mockWebSocket);
        } catch (NoSuchFieldException e) {
            fail("WebSocketConnectionManager's websocket field has changed.");
        } catch (IllegalAccessException e) {
            fail("This shouldn't happen.");
        }
    }

    /**
     * Test to check whether a subscription request from [webSocketConnectionManager] will correctly
     * marshall the enum field of the [EnumFieldSubscription]. If the "testEnum" field of the JSON
     * string sent to the mockWebSocket is null, this test will fail.
     */
    @Test
    public void testWebSocketConnectionManagerCorrectlyMarshalsSubscriptionsWithEnums() {
        webSocketConnectionManager.requestSubscription(testSubscription, mockCallback);
        ArgumentCaptor<String> sentStringCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockWebSocket).send(sentStringCaptor.capture());

        try {
            JSONObject sentJSON = new JSONObject(sentStringCaptor.getValue());
            JSONObject payload = sentJSON.getJSONObject("payload");
            String data = payload.getString("data");
            assertEquals("{\"query\":\"\",\"variables\":{\"testEnum\":\"TEST_ENUM\"}}", data);
        } catch (JSONException e) {
            fail("invalid JSON was sent: " + e.getLocalizedMessage());
        }
    }
}