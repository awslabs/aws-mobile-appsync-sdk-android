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
public final class UpdatePostInput {
  private final @Nonnull String id;

  private final Input<String> author;

  private final Input<String> title;

  private final Input<String> content;

  private final Input<String> url;

  private final Input<Integer> ups;

  private final Input<Integer> downs;

  private final Input<Integer> version;

  UpdatePostInput(@Nonnull String id, Input<String> author, Input<String> title,
      Input<String> content, Input<String> url, Input<Integer> ups, Input<Integer> downs,
      Input<Integer> version) {
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

  public @Nullable String author() {
    return this.author.value;
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

  public @Nullable Integer version() {
    return this.version.value;
  }

  public static Builder builder() {
    return new Builder();
  }

  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        writer.writeCustom("id", com.amazonaws.mobileconnectors.appsync.demo.type.CustomType.ID, id);
        if (author.defined) {
          writer.writeString("author", author.value);
        }
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
        if (version.defined) {
          writer.writeInt("version", version.value);
        }
      }
    };
  }

  public static final class Builder {
    private @Nonnull String id;

    private Input<String> author = Input.absent();

    private Input<String> title = Input.absent();

    private Input<String> content = Input.absent();

    private Input<String> url = Input.absent();

    private Input<Integer> ups = Input.absent();

    private Input<Integer> downs = Input.absent();

    private Input<Integer> version = Input.absent();

    Builder() {
    }

    public Builder id(@Nonnull String id) {
      this.id = id;
      return this;
    }

    public Builder author(@Nullable String author) {
      this.author = Input.fromNullable(author);
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

    public Builder version(@Nullable Integer version) {
      this.version = Input.fromNullable(version);
      return this;
    }

    public UpdatePostInput build() {
      Utils.checkNotNull(id, "id == null");
      return new UpdatePostInput(id, author, title, content, url, ups, downs, version);
    }
  }
}
