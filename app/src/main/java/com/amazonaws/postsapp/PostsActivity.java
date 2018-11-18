package com.amazonaws.postsapp;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.arch.lifecycle.ProcessLifecycleOwner;


import com.amazonaws.mobileconnectors.appsync.AWSAppSyncAppLifecycleObserver;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;

public class PostsActivity extends AppCompatActivity {
    private static final String TAG = PostsActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private PostsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private SwipeRefreshLayout mSwipeContainer;

    private AWSAppSyncClient mAWSAppSyncClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AWSAppSyncAppLifecycleObserver());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_posts);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new PostsAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeContainer = (SwipeRefreshLayout) findViewById(R.id.refreshLayout);
        mSwipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeContainer.setRefreshing(false);
            }
        });

        FloatingActionButton fabAddPost = (FloatingActionButton) findViewById(R.id.addPost);
        fabAddPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddPostActivity.startActivity(PostsActivity.this);
            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // TODO: Here for query

}
