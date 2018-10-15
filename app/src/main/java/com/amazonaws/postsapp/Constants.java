package com.amazonaws.postsapp;

import com.amazonaws.regions.Regions;

public class Constants {
    public static final Regions APPSYNC_REGION = Regions.US_EAST_1; // TODO: Update the region to match the API region
    public static final String APPSYNC_API_URL = ""; // TODO: Update the endpoint URL as specified on AppSync console

    // API Key Authorization
    public static final String APPSYNC_API_KEY = "API-KEY"; // TODO: Copy the API Key specified on the AppSync Console

    // IAM based Authorization (Cognito Identity)
    public static final String COGNITO_IDENTITY = ""; // TODO: Update the Cognito Identity Pool ID
    public static final Regions COGNITO_REGION = Regions.US_EAST_1; // TODO: Update the region to match the Cognito Identity Pool region

    // Cognito User Pools Authorization
    public static final String USER_POOLS_POOL_ID = "";
    public static final String USER_POOLS_CLIENT_ID = "";
    public static final String USER_POOLS_CLIENT_SECRET = "";
    public static final Regions USER_POOLS_REGION = Regions.US_WEST_2; // TODO: Update the region to match the Cognito User Pools region
}
