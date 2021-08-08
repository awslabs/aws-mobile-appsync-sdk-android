package com.amazonaws.postsapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;


import com.amazonaws.amplify.generated.graphql.ListPostsQuery;

public class UpdatePostActivity extends AppCompatActivity {
    private static ListPostsQuery.ListPost sPost;
    private static int sPosition;

    public static void startActivity(Context context, ListPostsQuery.ListPost post, int position) {
        Intent updatePostIntent = new Intent(context, UpdatePostActivity.class);
        sPost = post;
        sPosition = position;
        context.startActivity(updatePostIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_post);

        ((EditText) findViewById(R.id.updateTitle)).setText(sPost.title());
        ((EditText) findViewById(R.id.updateAuthor)).setText(sPost.author());
        ((EditText) findViewById(R.id.updateContent)).setText(sPost.content());
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
        // TODO: Here for update post mutation
    }
    // TODO: Here for update post callback

}
