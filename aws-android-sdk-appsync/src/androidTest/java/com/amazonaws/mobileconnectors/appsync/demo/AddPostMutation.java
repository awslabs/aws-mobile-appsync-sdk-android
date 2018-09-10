package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CreatePostInput;
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
public final class AddPostMutation implements Mutation<AddPostMutation.Data, AddPostMutation.Data, AddPostMutation.Variables> {
  public static final String OPERATION_DEFINITION = "mutation AddPost($input: CreatePostInput!) {\n"
      + "  createPost(input: $input) {\n"
      + "    __typename\n"
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
      return "AddPost";
    }
  };

  private final AddPostMutation.Variables variables;

  public AddPostMutation(@Nonnull CreatePostInput input) {
    Utils.checkNotNull(input, "input == null");
    variables = new AddPostMutation.Variables(input);
  }

  @Override
  public String operationId() {
    return "3db1851d0e4349cfd9c02a87032a51791424f838548f026c9e1f526672806f2f";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public AddPostMutation.Data wrapData(AddPostMutation.Data data) {
    return data;
  }

  @Override
  public AddPostMutation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<AddPostMutation.Data> responseFieldMapper() {
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
    private @Nonnull CreatePostInput input;

    Builder() {
    }

    public Builder input(@Nonnull CreatePostInput input) {
      this.input = input;
      return this;
    }

    public AddPostMutation build() {
      Utils.checkNotNull(input, "input == null");
      return new AddPostMutation(input);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull CreatePostInput input;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull CreatePostInput input) {
      this.input = input;
      this.valueMap.put("input", input);
    }

    public @Nonnull CreatePostInput input() {
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
      ResponseField.forObject("createPost", "createPost", new UnmodifiableMapBuilder<String, Object>(1)
        .put("input", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "input")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable CreatePost createPost;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable CreatePost createPost) {
      this.createPost = createPost;
    }

    public @Nullable CreatePost createPost() {
      return this.createPost;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], createPost != null ? createPost.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "createPost=" + createPost
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
        return ((this.createPost == null) ? (that.createPost == null) : this.createPost.equals(that.createPost));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (createPost == null) ? 0 : createPost.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final CreatePost.Mapper createPostFieldMapper = new CreatePost.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final CreatePost createPost = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<CreatePost>() {
          @Override
          public CreatePost read(ResponseReader reader) {
            return createPostFieldMapper.map(reader);
          }
        });
        return new Data(createPost);
      }
    }
  }

  public static class CreatePost {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nullable String title;

    final @Nonnull String author;

    final @Nullable String url;

    final @Nullable String content;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public CreatePost(@Nonnull String __typename, @Nullable String title, @Nonnull String author,
        @Nullable String url, @Nullable String content) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.title = title;
      this.author = Utils.checkNotNull(author, "author == null");
      this.url = url;
      this.content = content;
    }

    public @Nonnull String __typename() {
      return this.__typename;
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
          writer.writeString($responseFields[1], title);
          writer.writeString($responseFields[2], author);
          writer.writeString($responseFields[3], url);
          writer.writeString($responseFields[4], content);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "CreatePost{"
          + "__typename=" + __typename + ", "
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
      if (o instanceof CreatePost) {
        CreatePost that = (CreatePost) o;
        return this.__typename.equals(that.__typename)
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

    public static final class Mapper implements ResponseFieldMapper<CreatePost> {
      @Override
      public CreatePost map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String title = reader.readString($responseFields[1]);
        final String author = reader.readString($responseFields[2]);
        final String url = reader.readString($responseFields[3]);
        final String content = reader.readString($responseFields[4]);
        return new CreatePost(__typename, title, author, url, content);
      }
    }
  }
}
