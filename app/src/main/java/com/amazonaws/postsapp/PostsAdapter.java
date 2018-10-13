package com.amazonaws.postsapp;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.demo.posts.DeltaPostMutation;
import com.amazonaws.demo.posts.ListPostsQuery;
import com.amazonaws.demo.posts.OnCreatePostSubscription;
import com.amazonaws.demo.posts.OnUpdatePostSubscription;
import com.amazonaws.demo.posts.SyncPostsQuery;
import com.amazonaws.demo.posts.UpdatePostMutation;
import com.amazonaws.demo.posts.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.annotation.Nonnull;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;
        public TextView mAuthorView;
        public TextView mContentView;
        public TextView mUpsView;
        public TextView mDownsView;
        public TextView mShareView;

        public ViewHolder(View v) {
            super(v);
            mTitleView = (TextView) v.findViewById(R.id.postTitle);
            mAuthorView = (TextView) v.findViewById(R.id.postAuthor);
            mContentView = (TextView) v.findViewById(R.id.postContent);
            mUpsView = (TextView) v.findViewById(R.id.postUps);
            mDownsView = (TextView) v.findViewById(R.id.postDowns);
            mShareView = (TextView) v.findViewById(R.id.postUrl);
        }
    }

   class PostObject {
        String id;
        String author;
        String title;
        String content;
        String url;
        Integer ups;
        Integer downs;
        int version;
    }

    private Map<String, PostsAdapter.PostObject> mPosts ;
    private List<String> mPostIDs ;

    private PostsActivity display;
    private AppSyncSubscriptionCall onUpdatePostSubscriptionWatcher;

    private static final String TAG = PostsAdapter.class.getSimpleName();


    public PostsAdapter(final PostsActivity display) {
        this.display = display;
        mPosts = new HashMap<String, PostObject>(0);
        mPostIDs = new ArrayList<String>();

        //Create the client
        AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());


        //Base Query
        Query baseQuery = ListPostsQuery.builder().build();
        GraphQLCall.Callback baseQueryCallback = new GraphQLCall.Callback<ListPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<ListPostsQuery.Data> response) {
                if (response == null  || response.data() == null || response.data().listPosts() == null || response.data().listPosts().items() == null ) {
                    Log.d(TAG, "List Posts returned with no data");
                    return;
                }

                Log.d(TAG, "SyncPosts returned. Iterating over the data");
                display.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       for (ListPostsQuery.Item p: response.data().listPosts().items() ) {
                           PostsAdapter.PostObject item = new PostsAdapter.PostObject();
                           item.id = p.id();
                           item.author = p.author();
                           item.title = p.title();
                           item.content = p.content();
                           item.url = p.url();
                           item.ups = p.ups();
                           item.downs = p.downs();
                           item.version = p.version();
                           if (mPosts.get(item.id) == null ) {
                               mPostIDs.add(0, item.id);
                           }
                           mPosts.put(item.id, item);
                       }

                       notifyDataSetChanged();
                   }
               });

            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "ListPostsQuery failed with [" + e.getLocalizedMessage() + "]");
            }
        };
        
        //Setup Add Post Subscription
        OnCreatePostSubscription onCreatePostSubscription = OnCreatePostSubscription.builder().build();
        AppSyncSubscriptionCall.Callback onCreatePostCallback = new AppSyncSubscriptionCall.Callback<OnCreatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnCreatePostSubscription.Data> response) {
                Log.d(TAG, "New post received via subscription.");
                if (response == null  || response.data() == null || response.data().onCreatePost() == null ) {
                    Log.d(TAG, "NewPost was null!");
                    return;
                }

                Log.d(TAG, "Adding post to display");
                final OnCreatePostSubscription.OnCreatePost p = response.data().onCreatePost();
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        PostsAdapter.PostObject item = new PostsAdapter.PostObject();
                        item.id = p.id();
                        item.author = p.author();
                        item.title = p.title();
                        item.content = p.content();
                        item.url = p.url();
                        item.ups = p.ups();
                        item.downs = p.downs();
                        item.version = p.version();
                        //Only add to the list if this is a new item.
                        if (mPosts.get(item.id) == null) {
                            mPostIDs.add(0, item.id);
                        }
                        mPosts.put(item.id, item);
                        notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error " + e.getLocalizedMessage());
            }
            
            @Override
            public void onCompleted() {
                Log.d(TAG, "Received onCompleted on subscription");

            }
        };


        //Delta  Query
        Query deltaQuery = SyncPostsQuery.builder().lastSync("0").build();

        GraphQLCall.Callback deltaQueryCallback = new GraphQLCall.Callback<SyncPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<SyncPostsQuery.Data> response) {
                if (response == null  || response.data() == null || response.data().syncPosts() == null ) {
                    Log.d(TAG, "SyncPosts returned with no data");
                    return;
                }

                Log.d(TAG, "SyncPosts returned. Iterating over the data");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (SyncPostsQuery.SyncPost p: response.data().syncPosts() ) {
                            PostObject item;
                            if (  (item = mPosts.get(p.id())) == null ) {
                                item = new PostObject();
                                mPostIDs.add(0,p.id());
                            }
                            item.id = p.id();
                            item.author = p.author();
                            item.title = p.title();
                            item.content = p.content();
                            item.url = p.url();
                            item.ups = p.ups();
                            item.downs = p.downs();
                            item.version = p.version();
                            mPosts.put(item.id, item);
                        }
                        notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "SyncPosts Query failed with [" + e.getLocalizedMessage() + "]");
            }
        };


        client.deltaSync(baseQuery, baseQueryCallback, onCreatePostSubscription, onCreatePostCallback, deltaQuery, deltaQueryCallback, 20 * 60, 24 * 3600 );
        //client.deltaSync(baseQuery, baseQueryCallback, null, null, null , null, 20 * 60, 24 * 3600 );

        //Setup Update Post Subscription
        setupUpdatePostSubscription();
    }

    private void setupUpdatePostSubscription() {
        AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());
        OnUpdatePostSubscription onUpdatePostSubscription = OnUpdatePostSubscription.builder().build();
        AppSyncSubscriptionCall.Callback onUpdatePostCallback = new AppSyncSubscriptionCall.Callback<OnUpdatePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnUpdatePostSubscription.Data> response) {
                Log.d(TAG, "New update received via subscription.");
                if (response == null  || response.data() == null || response.data().onUpdatePost() == null ) {
                    Log.d(TAG, "Update was null!");
                    return;
                }

                final OnUpdatePostSubscription.OnUpdatePost p = response.data().onUpdatePost();

                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PostObject item;

                        if( (item = mPosts.get(p.id())) != null  && item.version < p.version()) {
                            item.author = p.author();
                            item.title = p.title();
                            item.content = p.content();
                            item.url = p.url();
                            item.ups = p.ups();
                            item.downs = p.downs();
                            item.version = p.version();
                        }
                        notifyDataSetChanged();
                    }
                });
            }


            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "Error " + e.getLocalizedMessage());
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "Received onCompleted on subscription");

            }
        };
        onUpdatePostSubscriptionWatcher = client.subscribe(onUpdatePostSubscription);
        onUpdatePostSubscriptionWatcher.execute(onUpdatePostCallback);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if ( mPosts.size() < position ) {
            return;
        }
        final PostObject item = mPosts.get(mPostIDs.get(position));
        holder.mTitleView.setText(item.title);
        holder.mAuthorView.setText(item.author);
        holder.mContentView.setText(item.content);
        holder.mUpsView.setText(item.ups + " Ups");
        holder.mDownsView.setText(item.downs + " Downs");
        holder.mShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_SUBJECT, item.url)
                        .putExtra(Intent.EXTRA_TEXT, item.url)
                        .setType("text/plain");
                view.getContext().startActivity(sendIntent);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdatePostActivity.startActivity(view.getContext(), item, position);
            }
        });
        holder.mUpsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Sending up vote", Toast.LENGTH_SHORT).show();
                upVotePost(view, position, item);
            }
        });
        holder.mDownsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Sending down vote", Toast.LENGTH_SHORT).show();
                downVotePost(view, position, item);
            }
        });
    }

    private void upVotePost(final View view, final int position, final PostObject post) {


        display.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                post.ups = post.ups + 1;
                post.version = post.version +1;
                notifyDataSetChanged();
            }
        });

        final UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(post.id)
                        .title(post.title)
                        .author(post.author)
                        .url(post.url)
                        .content(post.content)
                        .ups(post.ups)
                        .downs(post.downs)
                        .version(post.version)
                        .build())
                .build();
        ClientFactory.getInstance(view.getContext()).mutate(updatePostMutation).enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
               Log.d(TAG, "Up voted successfully");
               display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Up Voted!", Toast.LENGTH_SHORT).show();
                        updateDeltaEntry(ClientFactory.getInstance(display.getApplicationContext()), response);
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        post.ups = post.ups -1;
                        notifyDataSetChanged();
                        Toast.makeText(display, "Failed to upvote post!", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("", e.getMessage());
            }
        });

    }

    private void downVotePost(final View view, final int position, final PostObject post) {
        display.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                post.downs = post.downs + 1;
                post.version  = post.version + 1;
                notifyDataSetChanged();
            }
        });

        UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(post.id)
                        .title(post.title)
                        .author(post.author)
                        .url(post.url)
                        .content(post.content)
                        .ups(post.ups)
                        .downs(post.downs)
                        .version(post.version)
                        .build())
                .build();

        ClientFactory.getInstance(view.getContext()).mutate(updatePostMutation).enqueue(new GraphQLCall.Callback<UpdatePostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<UpdatePostMutation.Data> response) {
                Log.d(TAG, "Down voted successfully");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Down Voted!", Toast.LENGTH_SHORT).show();
                        updateDeltaEntry(ClientFactory.getInstance(display.getApplicationContext()), response);
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        post.downs = post.downs -1;
                        notifyDataSetChanged();
                        Toast.makeText(display, "Failed to Down Vote post!", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("", e.getMessage());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    private void updateDeltaEntry( AWSAppSyncClient client, Response <UpdatePostMutation.Data> response ) {
        DeltaPostMutation.Data expected = new DeltaPostMutation.Data(null);

        DeltaPostMutation deltaPostMutation = DeltaPostMutation.builder()
                .id(response.data().updatePost().id())
                .author(response.data().updatePost().author())
                .title(response.data().updatePost().title())
                .ups(response.data().updatePost().ups())
                .downs(response.data().updatePost().downs())
                .content(response.data().updatePost().content())
                .version(response.data().updatePost().version())
                .build();

        client.mutate(deltaPostMutation, expected)
                .enqueue(new GraphQLCall.Callback<DeltaPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<DeltaPostMutation.Data> response) {
                        Log.d(TAG, "Delta Post updated");
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Delta Post update failed with [" + e.getLocalizedMessage() + "]");
                    }
                });

    }
}
