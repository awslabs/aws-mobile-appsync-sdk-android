package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CustomType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class OnCreatePostSubscription implements Subscription<OnCreatePostSubscription.Data, OnCreatePostSubscription.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "subscription OnCreatePost {\n"
      + "  onCreatePost {\n"
      + "    __typename\n"
      + "    id\n"
      + "    author\n"
      + "    title\n"
      + "    content\n"
      + "    url\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "OnCreatePost";
    }
  };

  private final Operation.Variables variables;

  public OnCreatePostSubscription() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String operationId() {
    return "145145ba3426ace280abc21d2e2f7b131d9f7467d99390b442a5ede962c172e8";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public OnCreatePostSubscription.Data wrapData(OnCreatePostSubscription.Data data) {
    return data;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<OnCreatePostSubscription.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public OnCreatePostSubscription build() {
      return new OnCreatePostSubscription();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("onCreatePost", "onCreatePost", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable OnCreatePost onCreatePost;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable OnCreatePost onCreatePost) {
      this.onCreatePost = onCreatePost;
    }

    public @Nullable OnCreatePost onCreatePost() {
      return this.onCreatePost;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], onCreatePost != null ? onCreatePost.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "onCreatePost=" + onCreatePost
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.onCreatePost == null) ? (that.onCreatePost == null) : this.onCreatePost.equals(that.onCreatePost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (onCreatePost == null) ? 0 : onCreatePost.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final OnCreatePost.Mapper onCreatePostFieldMapper = new OnCreatePost.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final OnCreatePost onCreatePost = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<OnCreatePost>() {
          @Override
          public OnCreatePost read(ResponseReader reader) {
            return onCreatePostFieldMapper.map(reader);
          }
        });
        return new Data(onCreatePost);
      }
    }
  }

  public static class OnCreatePost {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nonnull String author;

    final @Nullable String title;

    final @Nullable String content;

    final @Nullable String url;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public OnCreatePost(@Nonnull String __typename, @Nonnull String id, @Nonnull String author,
        @Nullable String title, @Nullable String content, @Nullable String url) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.author = Utils.checkNotNull(author, "author == null");
      this.title = title;
      this.content = content;
      this.url = url;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String id() {
      return this.id;
    }

    public @Nonnull String author() {
      return this.author;
    }

    public @Nullable String title() {
      return this.title;
    }

    public @Nullable String content() {
      return this.content;
    }

    public @Nullable String url() {
      return this.url;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], id);
          writer.writeString($responseFields[2], author);
          writer.writeString($responseFields[3], title);
          writer.writeString($responseFields[4], content);
          writer.writeString($responseFields[5], url);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "OnCreatePost{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "author=" + author + ", "
          + "title=" + title + ", "
          + "content=" + content + ", "
          + "url=" + url
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof OnCreatePost) {
        OnCreatePost that = (OnCreatePost) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.author.equals(that.author)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && ((this.content == null) ? (that.content == null) : this.content.equals(that.content))
         && ((this.url == null) ? (that.url == null) : this.url.equals(that.url));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= id.hashCode();
        h *= 1000003;
        h ^= author.hashCode();
        h *= 1000003;
        h ^= (title == null) ? 0 : title.hashCode();
        h *= 1000003;
        h ^= (content == null) ? 0 : content.hashCode();
        h *= 1000003;
        h ^= (url == null) ? 0 : url.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<OnCreatePost> {
      @Override
      public OnCreatePost map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String author = reader.readString($responseFields[2]);
        final String title = reader.readString($responseFields[3]);
        final String content = reader.readString($responseFields[4]);
        final String url = reader.readString($responseFields[5]);
        return new OnCreatePost(__typename, id, author, title, content, url);
      }
    }
  }
}
