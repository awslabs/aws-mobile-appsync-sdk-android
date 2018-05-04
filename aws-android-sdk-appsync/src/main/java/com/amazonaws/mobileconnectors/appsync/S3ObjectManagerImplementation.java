/**
 * Copyright 2018-2018 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.mobileconnectors.appsync;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.apollographql.apollo.api.S3InputObjectInterface;
import com.apollographql.apollo.api.S3ObjectInterface;
import com.apollographql.apollo.api.S3ObjectManager;

import java.io.File;

/**
 * S3ObjectManagerImplementation.
 */

public class S3ObjectManagerImplementation implements S3ObjectManager {

    private AmazonS3Client s3Client;

    public S3ObjectManagerImplementation(AmazonS3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void upload(S3InputObjectInterface s3Object) throws Exception {
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
    public void download(S3ObjectInterface s3Object, String filePath) throws Exception {
        GetObjectRequest request = new GetObjectRequest(s3Object.bucket(), s3Object.key());
        s3Client.getObject(request, new File(filePath));
    }
}
