/**
 * Copyright 2018-2019 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazonaws.mobileconnectors.appsync;

import android.support.annotation.NonNull;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.S3ObjectInterface;
import com.apollographql.apollo.api.S3ObjectManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * S3ObjectManagerImplementation.
 */

public class S3ObjectManagerImplementation implements S3ObjectManager {

    private AmazonS3Client s3Client;
    private static final String TAG = S3ObjectManagerImplementation.class.getSimpleName();

    public S3ObjectManagerImplementation(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void upload(S3InputObjectInterface s3Object) throws AmazonClientException {
        PutObjectRequest request = new PutObjectRequest(s3Object.bucket(),
                s3Object.key(),
                new File(s3Object.localUri()));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("Content-Type", s3Object.mimeType());
        request.setMetadata(metadata);
        request.setFile(new File(s3Object.localUri()));
        s3Client.putObject(request);

    }

    @Override
    public void download(S3ObjectInterface s3Object, String filePath) throws AmazonClientException {
        GetObjectRequest request = new GetObjectRequest(s3Object.bucket(), s3Object.key());
        s3Client.getObject(request, new File(filePath));
    }

    //Look for S3InputObjects in the input
    @NonNull
    static List<S3InputObjectInterface> getS3ComplexObjects(Map<String, Object> variablesMap) {
        List<S3InputObjectInterface> s3Objects = new ArrayList<>();
        for (String key : variablesMap.keySet()) {
            Object obj = variablesMap.get(key);
            if (obj == null) {
                continue;
            }
            Log.v(TAG, "Thread:[" + Thread.currentThread().getId() + "]: Looking at Key [" + key + "] of type [" +
                    obj.getClass().getSimpleName() + "]");
            //Check if the object is itself a S3ObjectInterface
            if (implementsS3InputObjectInterface(obj.getClass())) {
                s3Objects.add((S3InputObjectInterface) variablesMap.get(key));
            }
            //If it is a map, recurse into it
            else if (obj instanceof Map) {
                try {
                    Map<String, Object> variablesHashMap = (Map<String, Object>) obj;
                    List<S3InputObjectInterface> s3ObjectList = getS3ComplexObjects(variablesHashMap);
                    s3Objects.addAll(s3ObjectList);
                } catch (ClassCastException e) {
                    Log.e(TAG, "Class cast exception " + e.getLocalizedMessage());
                }
            } else {
                for (Method method : obj.getClass().getMethods()) {
                    if (implementsS3InputObjectInterface(method.getReturnType())) {
                        Log.v(TAG, "Method [" + method.getName() + " implements S3InputObjectInterface");
                        try {
                            S3InputObjectInterface s3Object = (S3InputObjectInterface) method.invoke(obj);
                            if (s3Object != null) {
                                s3Objects.add(s3Object);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "There was an exception " + e.getLocalizedMessage());
                        }
                    }

                }

            }
        }
        return s3Objects;
    }

    static private boolean implementsS3InputObjectInterface(Class cls) {
        for(Class c: cls.getInterfaces()) {
            if (c == S3InputObjectInterface.class ) {
                return true;
            }
        }
        return false;
    }

}
