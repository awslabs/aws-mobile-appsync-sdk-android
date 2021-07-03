package com.amazonaws.mobileconnectors.appsync.util.subscriptions;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.Utils;


import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A testing class of a subscription with an enum field. Follows how apollo-generated subscriptions
 * would generate (as of 3.1.4) - with some methods removed that aren't relevant to testing.
 */
public final class EnumFieldSubscription implements Subscription<EnumFieldSubscription.Data, EnumFieldSubscription.Data, EnumFieldSubscription.Variables> {

  private final EnumFieldSubscription.Variables variables;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "";
    }
  };

  public EnumFieldSubscription(@Nonnull TestEnum testEnum) {
    Utils.checkNotNull(testEnum, "testEnum == null");
    variables = new EnumFieldSubscription.Variables(testEnum);
  }

  @Override
  public String operationId() { return ""; }

  @Override
  public String queryDocument() {
    return "";
  }

  @Override
  public EnumFieldSubscription.Data wrapData(EnumFieldSubscription.Data data) {
    return data;
  }

  @Override
  public EnumFieldSubscription.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<EnumFieldSubscription.Data> responseFieldMapper() {
    return null;
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Variables extends Operation.Variables {

    private final @Nonnull TestEnum testEnum;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull TestEnum testEnum) {
      this.testEnum = testEnum;
      this.valueMap.put("testEnum", testEnum);
    }

    public @Nonnull TestEnum testEnum() {
      return testEnum;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeString("testEnum", testEnum.name());
        }
      };
    }
  }

  public static class Data implements Operation.Data {

    @Override
    public ResponseFieldMarshaller marshaller() {
      return null;
    }
  }
}
