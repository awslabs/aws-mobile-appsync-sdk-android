package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CustomType;
import com.amazonaws.mobileconnectors.appsync.demo.type.DeletePostInput;
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
public final class DeletePostMutation implements Mutation<DeletePostMutation.Data, DeletePostMutation.Data, DeletePostMutation.Variables> {
  public static final String OPERATION_DEFINITION = "mutation DeletePost($input: DeletePostInput!) {\n"
      + "  deletePost(input: $input) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    title\n"
      + "    author\n"
      + "    url\n"
      + "    content\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "DeletePost";
    }
  };

  private final DeletePostMutation.Variables variables;

  public DeletePostMutation(@Nonnull DeletePostInput input) {
    Utils.checkNotNull(input, "input == null");
    variables = new DeletePostMutation.Variables(input);
  }

  @Override
  public String operationId() {
    return "af091d209bc13ca36b4540396e792f3e0968d7a7a715b34e4639e78cfffb3d0b";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public DeletePostMutation.Data wrapData(DeletePostMutation.Data data) {
    return data;
  }

  @Override
  public DeletePostMutation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<DeletePostMutation.Data> responseFieldMapper() {
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
    private @Nonnull DeletePostInput input;

    Builder() {
    }

    public Builder input(@Nonnull DeletePostInput input) {
      this.input = input;
      return this;
    }

    public DeletePostMutation build() {
      Utils.checkNotNull(input, "input == null");
      return new DeletePostMutation(input);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull DeletePostInput input;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull DeletePostInput input) {
      this.input = input;
      this.valueMap.put("input", input);
    }

    public @Nonnull DeletePostInput input() {
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
      ResponseField.forObject("deletePost", "deletePost", new UnmodifiableMapBuilder<String, Object>(1)
        .put("input", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "input")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable DeletePost deletePost;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable DeletePost deletePost) {
      this.deletePost = deletePost;
    }

    public @Nullable DeletePost deletePost() {
      return this.deletePost;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], deletePost != null ? deletePost.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "deletePost=" + deletePost
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
        return ((this.deletePost == null) ? (that.deletePost == null) : this.deletePost.equals(that.deletePost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (deletePost == null) ? 0 : deletePost.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final DeletePost.Mapper deletePostFieldMapper = new DeletePost.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final DeletePost deletePost = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<DeletePost>() {
          @Override
          public DeletePost read(ResponseReader reader) {
            return deletePostFieldMapper.map(reader);
          }
        });
        return new Data(deletePost);
      }
    }
  }

  public static class DeletePost {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nullable String title;

    final @Nonnull String author;

    final @Nullable String url;

    final @Nullable String content;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public DeletePost(@Nonnull String __typename, @Nonnull String id, @Nullable String title,
        @Nonnull String author, @Nullable String url, @Nullable String content) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.title = title;
      this.author = Utils.checkNotNull(author, "author == null");
      this.url = url;
      this.content = content;
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

    public @Nullable String url() {
      return this.url;
    }

    public @Nullable String content() {
      return this.content;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], id);
          writer.writeString($responseFields[2], title);
          writer.writeString($responseFields[3], author);
          writer.writeString($responseFields[4], url);
          writer.writeString($responseFields[5], content);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "DeletePost{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "title=" + title + ", "
          + "author=" + author + ", "
          + "url=" + url + ", "
          + "content=" + content
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof DeletePost) {
        DeletePost that = (DeletePost) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && this.author.equals(that.author)
         && ((this.url == null) ? (that.url == null) : this.url.equals(that.url))
         && ((this.content == null) ? (that.content == null) : this.content.equals(that.content));
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
        h ^= (url == null) ? 0 : url.hashCode();
        h *= 1000003;
        h ^= (content == null) ? 0 : content.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<DeletePost> {
      @Override
      public DeletePost map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String title = reader.readString($responseFields[2]);
        final String author = reader.readString($responseFields[3]);
        final String url = reader.readString($responseFields[4]);
        final String content = reader.readString($responseFields[5]);
        return new DeletePost(__typename, id, title, author, url, content);
      }
    }
  }
}
