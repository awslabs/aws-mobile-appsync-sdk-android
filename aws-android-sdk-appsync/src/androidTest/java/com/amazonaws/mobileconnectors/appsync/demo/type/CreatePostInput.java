package com.amazonaws.mobileconnectors.appsync.demo.type;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class CreatePostInput {
  private final @Nonnull String id;

  private final @Nonnull String author;

  private final Input<String> title;

  private final Input<String> content;

  private final Input<String> url;

  private final Input<Integer> ups;

  private final Input<Integer> downs;

  private final int version;

  CreatePostInput(@Nonnull String id, @Nonnull String author, Input<String> title,
      Input<String> content, Input<String> url, Input<Integer> ups, Input<Integer> downs,
      int version) {
    this.id = id;
    this.author = author;
    this.title = title;
    this.content = content;
    this.url = url;
    this.ups = ups;
    this.downs = downs;
    this.version = version;
  }

  public @Nonnull String id() {
    return this.id;
  }

  public @Nonnull String author() {
    return this.author;
  }

  public @Nullable String title() {
    return this.title.value;
  }

  public @Nullable String content() {
    return this.content.value;
  }

  public @Nullable String url() {
    return this.url.value;
  }

  public @Nullable Integer ups() {
    return this.ups.value;
  }

  public @Nullable Integer downs() {
    return this.downs.value;
  }

  public int version() {
    return this.version;
  }

  public static Builder builder() {
    return new Builder();
  }

  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeCustom("id", com.amazonaws.mobileconnectors.appsync.demo.type.CustomType.ID, id);
        writer.writeString("author", author);
        if (title.defined) {
          writer.writeString("title", title.value);
        }
        if (content.defined) {
          writer.writeString("content", content.value);
        }
        if (url.defined) {
          writer.writeString("url", url.value);
        }
        if (ups.defined) {
          writer.writeInt("ups", ups.value);
        }
        if (downs.defined) {
          writer.writeInt("downs", downs.value);
        }
        writer.writeInt("version", version);
      }
    };
  }

  public static final class Builder {
    private @Nonnull String id;

    private @Nonnull String author;

    private Input<String> title = Input.absent();

    private Input<String> content = Input.absent();

    private Input<String> url = Input.absent();

    private Input<Integer> ups = Input.absent();

    private Input<Integer> downs = Input.absent();

    private int version;

    Builder() {
    }

    public Builder id(@Nonnull String id) {
      this.id = id;
      return this;
    }

    public Builder author(@Nonnull String author) {
      this.author = author;
      return this;
    }

    public Builder title(@Nullable String title) {
      this.title = Input.fromNullable(title);
      return this;
    }

    public Builder content(@Nullable String content) {
      this.content = Input.fromNullable(content);
      return this;
    }

    public Builder url(@Nullable String url) {
      this.url = Input.fromNullable(url);
      return this;
    }

    public Builder ups(@Nullable Integer ups) {
      this.ups = Input.fromNullable(ups);
      return this;
    }

    public Builder downs(@Nullable Integer downs) {
      this.downs = Input.fromNullable(downs);
      return this;
    }

    public Builder version(int version) {
      this.version = version;
      return this;
    }

    public CreatePostInput build() {
      Utils.checkNotNull(author, "author == null");
      return new CreatePostInput(id, author, title, content, url, ups, downs, version);
    }
  }
}
