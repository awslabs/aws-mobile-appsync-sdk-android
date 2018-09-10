package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CustomType;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class GetPostQuery implements Query<GetPostQuery.Data, GetPostQuery.Data, GetPostQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query GetPost($id: ID!) {\n"
      + "  getPost(id: $id) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    title\n"
      + "    author\n"
      + "    content\n"
      + "    url\n"
      + "    version\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "GetPost";
    }
  };

  private final GetPostQuery.Variables variables;

  public GetPostQuery(@Nonnull String id) {
    Utils.checkNotNull(id, "id == null");
    variables = new GetPostQuery.Variables(id);
  }

  @Override
  public String operationId() {
    return "e9836504a7038bbbfdf95c7344a3cefce8133698b3c98c9219a7b544eb174273";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public GetPostQuery.Data wrapData(GetPostQuery.Data data) {
    return data;
  }

  @Override
  public GetPostQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<GetPostQuery.Data> responseFieldMapper() {
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
    private @Nonnull String id;

    Builder() {
    }

    public Builder id(@Nonnull String id) {
      this.id = id;
      return this;
    }

    public GetPostQuery build() {
      Utils.checkNotNull(id, "id == null");
      return new GetPostQuery(id);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull String id;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull String id) {
      this.id = id;
      this.valueMap.put("id", id);
    }

    public @Nonnull String id() {
      return id;
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
          writer.writeCustom("id", com.amazonaws.mobileconnectors.appsync.demo.type.CustomType.ID, id);
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("getPost", "getPost", new UnmodifiableMapBuilder<String, Object>(1)
        .put("id", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "id")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable GetPost getPost;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable GetPost getPost) {
      this.getPost = getPost;
    }

    public @Nullable GetPost getPost() {
      return this.getPost;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], getPost != null ? getPost.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "getPost=" + getPost
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
        return ((this.getPost == null) ? (that.getPost == null) : this.getPost.equals(that.getPost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (getPost == null) ? 0 : getPost.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final GetPost.Mapper getPostFieldMapper = new GetPost.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final GetPost getPost = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<GetPost>() {
          @Override
          public GetPost read(ResponseReader reader) {
            return getPostFieldMapper.map(reader);
          }
        });
        return new Data(getPost);
      }
    }
  }

  public static class GetPost {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("version", "version", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nullable String title;

    final @Nonnull String author;

    final @Nullable String content;

    final @Nullable String url;

    final int version;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public GetPost(@Nonnull String __typename, @Nonnull String id, @Nullable String title,
        @Nonnull String author, @Nullable String content, @Nullable String url, int version) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.title = title;
      this.author = Utils.checkNotNull(author, "author == null");
      this.content = content;
      this.url = url;
      this.version = version;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String id() {
      return this.id;
    }

    public @Nullable String title() {
      return this.title;
    }

    public @Nonnull String author() {
      return this.author;
    }

    public @Nullable String content() {
      return this.content;
    }

    public @Nullable String url() {
      return this.url;
    }

    public int version() {
      return this.version;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], id);
          writer.writeString($responseFields[2], title);
          writer.writeString($responseFields[3], author);
          writer.writeString($responseFields[4], content);
          writer.writeString($responseFields[5], url);
          writer.writeInt($responseFields[6], version);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "GetPost{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "title=" + title + ", "
          + "author=" + author + ", "
          + "content=" + content + ", "
          + "url=" + url + ", "
          + "version=" + version
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof GetPost) {
        GetPost that = (GetPost) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && this.author.equals(that.author)
         && ((this.content == null) ? (that.content == null) : this.content.equals(that.content))
         && ((this.url == null) ? (that.url == null) : this.url.equals(that.url))
         && this.version == that.version;
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
        h ^= (title == null) ? 0 : title.hashCode();
        h *= 1000003;
        h ^= author.hashCode();
        h *= 1000003;
        h ^= (content == null) ? 0 : content.hashCode();
        h *= 1000003;
        h ^= (url == null) ? 0 : url.hashCode();
        h *= 1000003;
        h ^= version;
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<GetPost> {
      @Override
      public GetPost map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String title = reader.readString($responseFields[2]);
        final String author = reader.readString($responseFields[3]);
        final String content = reader.readString($responseFields[4]);
        final String url = reader.readString($responseFields[5]);
        final int version = reader.readInt($responseFields[6]);
        return new GetPost(__typename, id, title, author, content, url, version);
      }
    }
  }
}
