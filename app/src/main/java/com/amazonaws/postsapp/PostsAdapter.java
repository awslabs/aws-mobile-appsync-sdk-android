package com.amazonaws.postsapp;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.demo.posts.DeletePostMutation;
import com.amazonaws.demo.posts.DeltaPostMutation;
import com.amazonaws.demo.posts.ListPostsQuery;
import com.amazonaws.demo.posts.OnCreatePostSubscription;
import com.amazonaws.demo.posts.OnDeletePostSubscription;
import com.amazonaws.demo.posts.OnUpdatePostSubscription;
import com.amazonaws.demo.posts.SyncPostsQuery;
import com.amazonaws.demo.posts.UpdatePostMutation;
import com.amazonaws.demo.posts.type.DeletePostInput;
import com.amazonaws.demo.posts.type.UpdatePostInput;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;
        public TextView mAuthorView;
        public TextView mContentView;
        public TextView mUpsView;
        public TextView mDownsView;
        public TextView mDeleteView;

        public ViewHolder(View v) {
            super(v);
            mTitleView = (TextView) v.findViewById(R.id.postTitle);
            mAuthorView = (TextView) v.findViewById(R.id.postAuthor);
            mContentView = (TextView) v.findViewById(R.id.postContent);
            mUpsView = (TextView) v.findViewById(R.id.postUps);
            mDownsView = (TextView) v.findViewById(R.id.postDowns);
            mDeleteView = (TextView)v.findViewById(R.id.deletePost);
        }
    }

    private List<String> mPostIDs ;
    private Map<String, ListPostsQuery.Item> allPosts;

    private PostsActivity display;
    private AppSyncSubscriptionCall onUpdatePostSubscriptionWatcher;
    private AppSyncSubscriptionCall onDeletePostSubscriptionWatcher;

    private static final String TAG = PostsAdapter.class.getSimpleName();
    private Query listPostsQuery = null;

    public PostsAdapter(final PostsActivity display) {
        this.display = display;

        mPostIDs = new ArrayList<String>();
        allPosts = new HashMap<String, ListPostsQuery.Item>();

        //Create the client
        final AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());


        //Setup the List Posts Query
        listPostsQuery = ListPostsQuery.builder().build();
        GraphQLCall.Callback listPostsQueryCallback = new GraphQLCall.Callback<ListPostsQuery.Data>() {
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
                           //Populate the allPosts map.
                           if ( allPosts.get(p.id()) == null )  {
                               mPostIDs.add(0, p.id());
                           }
                           allPosts.put(p.id(), p);
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
                        if (allPosts.get(p.id()) == null ) {
                            mPostIDs.add(0, p.id());
                        }
                        allPosts.put(p.id(), new ListPostsQuery.Item("Post", p.id(), p.author(),p.title(),p.content(), p.url(), p.ups(), p.downs(),p.version()));
                        updateListPostsQueryCache();
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


        //Setup the Delta Query to get posts incrementally
        Query syncPostsQuery = SyncPostsQuery.builder().lastSync(0).build();

        GraphQLCall.Callback syncPostsQueryCallback = new GraphQLCall.Callback<SyncPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<SyncPostsQuery.Data> response) {
                if (response == null  || response.data() == null || response.data().syncPosts() == null ) {
                    Log.d(TAG, "SyncPosts returned with no data");
                    return;
                }

                //Add to the ListPostsQuery Cache
                Log.d(TAG, "SyncPosts returned. Iterating over the data");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (SyncPostsQuery.SyncPost p: response.data().syncPosts() ) {
                            if ( "DELETE".equalsIgnoreCase(p.aws_ds())) {
                                mPostIDs.remove(p.id());
                                allPosts.remove(p.id());
                                continue;
                            }

                            if (allPosts.get(p.id()) == null ) {
                                mPostIDs.add(0, p.id());
                            }
                            allPosts.put(p.id(), new ListPostsQuery.Item("Post", p.id(), p.author(),p.title(),p.content(), p.url(), p.ups(), p.downs(),p.version()));
                        }
                        updateListPostsQueryCache();
                        notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "SyncPosts Query failed with [" + e.getLocalizedMessage() + "]");
            }
        };

        Cancelable handle = client.sync(listPostsQuery, listPostsQueryCallback, onCreatePostSubscription, onCreatePostCallback, syncPostsQuery, syncPostsQueryCallback, 20 * 60 );

        //Setup the Update and Delete Post Subscriptions in a background thread
        new Thread( new Runnable() {
            @Override
            public void run() {
                //Setup Update Post Subscription
                setupUpdatePostSubscription();

                //Setup Delete Post Subscription
                setupDeletePostSubscription();
            }
        }).start();
    }

    private void setupUpdatePostSubscription() {
        final AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());

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
                        allPosts.put(p.id(), new ListPostsQuery.Item("Post", p.id(), p.author(),p.title(),p.content(), p.url(), p.ups(), p.downs(),p.version()));
                        updateListPostsQueryCache();
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


    private void setupDeletePostSubscription() {
        final AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());

        OnDeletePostSubscription onDeletePostSubscription = OnDeletePostSubscription.builder().build();

        AppSyncSubscriptionCall.Callback onDeletePostCallback = new AppSyncSubscriptionCall.Callback<OnDeletePostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnDeletePostSubscription.Data> response) {
                Log.d(TAG, "New delete received via subscription.");
                if (response == null  || response.data() == null || response.data().onDeletePost() == null ) {
                    Log.d(TAG, "Delete was null!");
                    return;
                }

                final OnDeletePostSubscription.OnDeletePost p = response.data().onDeletePost();

                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allPosts.remove(p.id());
                        mPostIDs.remove(p.id());
                        updateListPostsQueryCache();
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

        onDeletePostSubscriptionWatcher = client.subscribe(onDeletePostSubscription);
        onDeletePostSubscriptionWatcher.execute(onDeletePostCallback);
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final int iPosition = position;
        if ( mPostIDs.size() < iPosition ) {
            return;
        }

        final ListPostsQuery.Item  item  = allPosts.get(mPostIDs.get(position));
        if (item == null ) {
            return;
        }

        holder.mTitleView.setText(item.title());
        holder.mAuthorView.setText(item.author());
        holder.mContentView.setText(item.content());
        holder.mUpsView.setText(item.ups() + " Ups");
        holder.mDownsView.setText(item.downs() + " Downs");

        holder.mUpsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Sending up vote", Toast.LENGTH_SHORT).show();
                upVotePost(view, iPosition, item);
            }
        });
        holder.mDownsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Sending down vote", Toast.LENGTH_SHORT).show();
                downVotePost(view, iPosition, item);
            }
        });

        holder.mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Deleting post", Toast.LENGTH_SHORT).show();
                deletePost(view, iPosition, item);
            }
        });
    }

    private void deletePost(final View view, final int position, final ListPostsQuery.Item p) {
        //Execute Mutation
        DeletePostInput input = DeletePostInput.builder().id(p.id()).build();
        final DeletePostMutation deletePostMutation = DeletePostMutation.builder()
                .input(input)
                .build();
        ClientFactory.getInstance(view.getContext()).mutate(deletePostMutation).enqueue(new GraphQLCall.Callback<DeletePostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<DeletePostMutation.Data> response) {
                Log.d(TAG, "Deleted successfully");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Deleted!", Toast.LENGTH_SHORT).show();
                        markPostAsDeletedInDeltaTable(ClientFactory.getInstance(display.getApplicationContext()), p);
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Failed to Delete post!", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("", e.getMessage());
            }
        });

        //Update listPostsQuery cache for optimistic UI
        display.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPostIDs.remove(position);
                allPosts.remove(p.id());
                updateListPostsQueryCache();
                notifyDataSetChanged();
            }
        });
    }

    private void upVotePost(final View view, final int position, final ListPostsQuery.Item p) {

        //Execute Mutation
        final UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(p.id())
                        .title(p.title())
                        .author(p.author())
                        .url(p.url())
                        .content(p.content())
                        .ups(p.ups() + 1)
                        .downs(p.downs())
                        .version(p.version() + 1)
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
                        updateDeltaPost(ClientFactory.getInstance(display.getApplicationContext()), response);
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Failed to upvote post!", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("", e.getMessage());
            }
        });

        //Update listPostsQuery cache for optimistic UI
        display.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                allPosts.put(p.id(), new ListPostsQuery.Item("Post", p.id(), p.author(),p.title(),p.content(), p.url(), p.ups() + 1, p.downs(),p.version()+1));
                updateListPostsQueryCache();
                notifyDataSetChanged();
            }
        });

    }

    private void downVotePost(final View view, final int position, final ListPostsQuery.Item p) {
        //Execute Mutation
        UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(p.id())
                        .title(p.title())
                        .author(p.author())
                        .url(p.url())
                        .content(p.content())
                        .ups(p.ups())
                        .downs(p.downs()+1)
                        .version(p.version()+1)
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
                        updateDeltaPost(ClientFactory.getInstance(display.getApplicationContext()), response);
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull final ApolloException e) {
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Failed to Down Vote post!", Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("", e.getMessage());
            }
        });

        //Update listPostsQuery cache for optimistic UI
        display.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                allPosts.put(p.id(), new ListPostsQuery.Item("Post", p.id(), p.author(),p.title(),p.content(), p.url(), p.ups(), p.downs() +1 ,p.version() + 1));
                updateListPostsQueryCache();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPostIDs.size();
    }

    private void updateDeltaPost(AWSAppSyncClient client, Response <UpdatePostMutation.Data> response ) {
        DeltaPostMutation.Data expected = new DeltaPostMutation.Data(null);

        DeltaPostMutation deltaPostMutation = DeltaPostMutation.builder()
                .id(response.data().updatePost().id())
                .author(response.data().updatePost().author())
                .title(response.data().updatePost().title())
                .ups(response.data().updatePost().ups())
                .downs(response.data().updatePost().downs())
                .content(response.data().updatePost().content())
                .version(response.data().updatePost().version())
                .aws_ds("UPDATE")
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

    private void markPostAsDeletedInDeltaTable(AWSAppSyncClient client, ListPostsQuery.Item p) {
        DeltaPostMutation.Data expected = new DeltaPostMutation.Data(null);

        DeltaPostMutation deltaPostMutation = DeltaPostMutation.builder()
                .id(p.id())
                .author(p.author())
                .title(p.title())
                .ups(p.ups())
                .downs(p.downs())
                .content(p.content())
                .version(p.version())
                .aws_ds("DELETE")
                .build();

        client.mutate(deltaPostMutation, expected)
                .enqueue(new GraphQLCall.Callback<DeltaPostMutation.Data>() {
                    @Override
                    public void onResponse(@Nonnull Response<DeltaPostMutation.Data> response) {
                        Log.d(TAG, "Delta Post marked as deleted");
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.d(TAG, "Delta Post mark as deleted failed with [" + e.getLocalizedMessage() + "]");
                    }
                });
    }

    private void updateListPostsQueryCache() {
        List<ListPostsQuery.Item> items = new ArrayList<>();
        items.addAll(allPosts.values());
        ListPostsQuery.Data data  = new ListPostsQuery.Data( new ListPostsQuery.ListPosts("PostConnection",items, null));
        ClientFactory.getInstance(display.getApplicationContext()).getStore().write(listPostsQuery, data).enqueue(null);
    }
}
