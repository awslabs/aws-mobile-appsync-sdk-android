package com.amazonaws.mobileconnectors.appsync.demo;

import com.amazonaws.mobileconnectors.appsync.demo.type.CustomType;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class AllPostsQuery implements Query<AllPostsQuery.Data, AllPostsQuery.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query AllPosts {\n"
      + "  listPosts {\n"
      + "    __typename\n"
      + "    items {\n"
      + "      __typename\n"
      + "      id\n"
      + "      title\n"
      + "      author\n"
      + "      content\n"
      + "      url\n"
      + "      version\n"
      + "      ups\n"
      + "      downs\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "AllPosts";
    }
  };

  private final Operation.Variables variables;

  public AllPostsQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String operationId() {
    return "9cce58993fe26d1393749d8271d9ca134ef2d09465da47e056ffc2457243b314";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public AllPostsQuery.Data wrapData(AllPostsQuery.Data data) {
    return data;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<AllPostsQuery.Data> responseFieldMapper() {
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

    public AllPostsQuery build() {
      return new AllPostsQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("listPosts", "listPosts", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable ListPosts listPosts;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable ListPosts listPosts) {
      this.listPosts = listPosts;
    }

    public @Nullable ListPosts listPosts() {
      return this.listPosts;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], listPosts != null ? listPosts.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "listPosts=" + listPosts
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
        return ((this.listPosts == null) ? (that.listPosts == null) : this.listPosts.equals(that.listPosts));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (listPosts == null) ? 0 : listPosts.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final ListPosts.Mapper listPostsFieldMapper = new ListPosts.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final ListPosts listPosts = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<ListPosts>() {
          @Override
          public ListPosts read(ResponseReader reader) {
            return listPostsFieldMapper.map(reader);
          }
        });
        return new Data(listPosts);
      }
    }
  }

  public static class ListPosts {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("items", "items", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nullable List<Item> items;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public ListPosts(@Nonnull String __typename, @Nullable List<Item> items) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.items = items;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nullable List<Item> items() {
      return this.items;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], items, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Item) value).marshaller());
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "ListPosts{"
          + "__typename=" + __typename + ", "
          + "items=" + items
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof ListPosts) {
        ListPosts that = (ListPosts) o;
        return this.__typename.equals(that.__typename)
         && ((this.items == null) ? (that.items == null) : this.items.equals(that.items));
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
        h ^= (items == null) ? 0 : items.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<ListPosts> {
      final Item.Mapper itemFieldMapper = new Item.Mapper();

      @Override
      public ListPosts map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Item> items = reader.readList($responseFields[1], new ResponseReader.ListReader<Item>() {
          @Override
          public Item read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Item>() {
              @Override
              public Item read(ResponseReader reader) {
                return itemFieldMapper.map(reader);
              }
            });
          }
        });
        return new ListPosts(__typename, items);
      }
    }
  }

  public static class Item {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("title", "title", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("author", "author", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("content", "content", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("url", "url", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("version", "version", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("ups", "ups", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("downs", "downs", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nullable String title;

    final @Nonnull String author;

    final @Nullable String content;

    final @Nullable String url;

    final int version;

    final @Nullable Integer ups;

    final @Nullable Integer downs;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Item(@Nonnull String __typename, @Nonnull String id, @Nullable String title,
        @Nonnull String author, @Nullable String content, @Nullable String url, int version,
        @Nullable Integer ups, @Nullable Integer downs) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.title = title;
      this.author = Utils.checkNotNull(author, "author == null");
      this.content = content;
      this.url = url;
      this.version = version;
      this.ups = ups;
      this.downs = downs;
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

    public @Nullable Integer ups() {
      return this.ups;
    }

    public @Nullable Integer downs() {
      return this.downs;
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
          writer.writeInt($responseFields[7], ups);
          writer.writeInt($responseFields[8], downs);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Item{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "title=" + title + ", "
          + "author=" + author + ", "
          + "content=" + content + ", "
          + "url=" + url + ", "
          + "version=" + version + ", "
          + "ups=" + ups + ", "
          + "downs=" + downs
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Item) {
        Item that = (Item) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
         && this.author.equals(that.author)
         && ((this.content == null) ? (that.content == null) : this.content.equals(that.content))
         && ((this.url == null) ? (that.url == null) : this.url.equals(that.url))
         && this.version == that.version
         && ((this.ups == null) ? (that.ups == null) : this.ups.equals(that.ups))
         && ((this.downs == null) ? (that.downs == null) : this.downs.equals(that.downs));
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
        h *= 1000003;
        h ^= (ups == null) ? 0 : ups.hashCode();
        h *= 1000003;
        h ^= (downs == null) ? 0 : downs.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Item> {
      @Override
      public Item map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String title = reader.readString($responseFields[2]);
        final String author = reader.readString($responseFields[3]);
        final String content = reader.readString($responseFields[4]);
        final String url = reader.readString($responseFields[5]);
        final int version = reader.readInt($responseFields[6]);
        final Integer ups = reader.readInt($responseFields[7]);
        final Integer downs = reader.readInt($responseFields[8]);
        return new Item(__typename, id, title, author, content, url, version, ups, downs);
      }
    }
  }
}
