package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CustomType;
import com.amazonaws.mobileconnectors.appsync.demo.type.UpdatePostInput;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
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
public final class UpdatePostMutation implements Mutation<UpdatePostMutation.Data, UpdatePostMutation.Data, UpdatePostMutation.Variables> {
  public static final String OPERATION_DEFINITION = "mutation UpdatePost($input: UpdatePostInput!) {\n"
      + "  updatePost(input: $input) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    author\n"
      + "    title\n"
      + "    content\n"
      + "    url\n"
      + "    version\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "UpdatePost";
    }
  };

  private final UpdatePostMutation.Variables variables;

  public UpdatePostMutation(@Nonnull UpdatePostInput input) {
    Utils.checkNotNull(input, "input == null");
    variables = new UpdatePostMutation.Variables(input);
  }

  @Override
  public String operationId() {
    return "849694624893284bd3e429af676afd039a37f5b517c4049735fc3e1620ccfb6a";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public UpdatePostMutation.Data wrapData(UpdatePostMutation.Data data) {
    return data;
  }

  @Override
  public UpdatePostMutation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<UpdatePostMutation.Data> responseFieldMapper() {
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
    private @Nonnull UpdatePostInput input;

    Builder() {
    }

    public Builder input(@Nonnull UpdatePostInput input) {
      this.input = input;
      return this;
    }

    public UpdatePostMutation build() {
      Utils.checkNotNull(input, "input == null");
      return new UpdatePostMutation(input);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull UpdatePostInput input;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull UpdatePostInput input) {
      this.input = input;
      this.valueMap.put("input", input);
    }

    public @Nonnull UpdatePostInput input() {
      return input;
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
          writer.writeObject("input", input.marshaller());
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("updatePost", "updatePost", new UnmodifiableMapBuilder<String, Object>(1)
        .put("input", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "input")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable UpdatePost updatePost;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable UpdatePost updatePost) {
      this.updatePost = updatePost;
    }

    public @Nullable UpdatePost updatePost() {
      return this.updatePost;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], updatePost != null ? updatePost.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "updatePost=" + updatePost
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
        return ((this.updatePost == null) ? (that.updatePost == null) : this.updatePost.equals(that.updatePost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (updatePost == null) ? 0 : updatePost.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final UpdatePost.Mapper updatePostFieldMapper = new UpdatePost.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final UpdatePost updatePost = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<UpdatePost>() {
          @Override
          public UpdatePost read(ResponseReader reader) {
            return updatePostFieldMapper.map(reader);
          }
        });
        return new Data(updatePost);
      }
    }
  }

  public static class UpdatePost {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("version", "version", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nonnull String author;

    final @Nullable String title;

    final @Nullable String content;

    final @Nullable String url;

    final int version;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public UpdatePost(@Nonnull String __typename, @Nonnull String id, @Nonnull String author,
        @Nullable String title, @Nullable String content, @Nullable String url, int version) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.author = Utils.checkNotNull(author, "author == null");
      this.title = title;
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

    public int version() {
      return this.version;
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
          writer.writeInt($responseFields[6], version);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "UpdatePost{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "author=" + author + ", "
          + "title=" + title + ", "
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
      if (o instanceof UpdatePost) {
        UpdatePost that = (UpdatePost) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.author.equals(that.author)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
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
        h ^= author.hashCode();
        h *= 1000003;
        h ^= (title == null) ? 0 : title.hashCode();
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

    public static final class Mapper implements ResponseFieldMapper<UpdatePost> {
      @Override
      public UpdatePost map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String author = reader.readString($responseFields[2]);
        final String title = reader.readString($responseFields[3]);
        final String content = reader.readString($responseFields[4]);
        final String url = reader.readString($responseFields[5]);
        final int version = reader.readInt($responseFields[6]);
        return new UpdatePost(__typename, id, author, title, content, url, version);
      }
    }
  }
}
