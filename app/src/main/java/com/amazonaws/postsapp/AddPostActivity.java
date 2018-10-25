package com.amazonaws.postsapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.demo.posts.CreatePostMutation;
import com.amazonaws.demo.posts.DeltaPostMutation;
import com.amazonaws.demo.posts.type.CreatePostInput;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

public class AddPostActivity extends AppCompatActivity {
    private static final String TAG = AddPostActivity.class.getSimpleName();

    public static void startActivity(Context context) {
        Intent updatePostIntent = new Intent(context, AddPostActivity.class);
        context.startActivity(updatePostIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        ((EditText) findViewById(R.id.updateTitle)).setText("1");
        ((EditText) findViewById(R.id.updateAuthor)).setText("Author");
        ((EditText) findViewById(R.id.updateUrl)).setText("https://testurl.com");
        ((EditText) findViewById(R.id.updateContent)).setText("Sample text Sample text Sample text");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save) {
            save();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void save() {
        final AddPostActivity display = this;
        String title = ((EditText) findViewById(R.id.updateTitle)).getText().toString();
        String author = ((EditText) findViewById(R.id.updateAuthor)).getText().toString();
        String url = ((EditText) findViewById(R.id.updateUrl)).getText().toString();
        String content = ((EditText) findViewById(R.id.updateContent)).getText().toString();


        final AWSAppSyncClient client = ClientFactory.getInstance(this.getApplicationContext());
        CreatePostMutation.Data expected = new CreatePostMutation.Data(null);
        CreatePostInput createPostInput = CreatePostInput.builder()
                .title(title)
                .author(author)
                .content(content)
                .url(url)
                .ups(0)
                .downs(0)
                .version(0)
                .build();
        CreatePostMutation addPostMutation = CreatePostMutation.builder().input(createPostInput).build();
        client.mutate(addPostMutation, expected)
                .enqueue(new GraphQLCall.Callback<CreatePostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull final Response<CreatePostMutation.Data> response) {
                        Log.d(TAG, "Post added");
                        //Add To Delta Table
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(display, "Added!", Toast.LENGTH_SHORT).show();
                                createDeltaEntry(client, response);
                                display.finish();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(display, "Failed to add post!", Toast.LENGTH_SHORT).show();
                                display.finish();
                            }
                        });
                        Log.d(TAG, "Post add failed with [" + e.getLocalizedMessage() + "]");
                        e.printStackTrace();
                    }
                });

    }

    private void createDeltaEntry(AWSAppSyncClient client, Response<CreatePostMutation.Data> response) {

        DeltaPostMutation.Data expected = new DeltaPostMutation.Data(null);

        DeltaPostMutation deltaPostMutation = DeltaPostMutation.builder()
                .id(response.data().createPost().id())
                .author(response.data().createPost().author())
                .title(response.data().createPost().title())
                .ups(response.data().createPost().ups())
                .downs(response.data().createPost().downs())
                .content(response.data().createPost().content())
                .version(response.data().createPost().version())
                .url(response.data().createPost().url())
                .aws_ds("PUT")
                .build();

        client.mutate(deltaPostMutation, expected)
                .enqueue(new GraphQLCall.Callback<DeltaPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<DeltaPostMutation.Data> response) {
                        Log.d(TAG, "Delta Post added");
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Delta Post add failed with [" + e.getLocalizedMessage() + "]");
                    }
                });

    }

}
