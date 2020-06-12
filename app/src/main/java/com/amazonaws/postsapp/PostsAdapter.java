package com.amazonaws.postsapp;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.amplify.generated.graphql.DeletePostMutation;
import com.amazonaws.amplify.generated.graphql.ListPostsDeltaQuery;
import com.amazonaws.amplify.generated.graphql.ListPostsQuery;

import com.amazonaws.amplify.generated.graphql.OnDeltaPostSubscription;
import com.amazonaws.amplify.generated.graphql.UpdatePostMutation;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.AppSyncSubscriptionCall;
import com.amazonaws.apollographql.apollo.GraphQLCall;
import com.amazonaws.apollographql.apollo.api.Query;
import com.amazonaws.apollographql.apollo.api.Response;
import com.amazonaws.apollographql.apollo.exception.ApolloException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import type.DeltaAction;
import type.UpdatePostInput;

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
    private Map<String, ListPostsQuery.ListPost> allPosts;

    private PostsActivity display;
    private AppSyncSubscriptionCall onUpdatePostSubscriptionWatcher;
    private AppSyncSubscriptionCall onDeletePostSubscriptionWatcher;

    private static final String TAG = PostsAdapter.class.getSimpleName();
    private Query listPostsQuery = null;

    public PostsAdapter(final PostsActivity display) {
        this.display = display;

        mPostIDs = new ArrayList<String>();
        allPosts = new HashMap<String, ListPostsQuery.ListPost>();

        //Create the client
        final AWSAppSyncClient client = ClientFactory.getInstance(display.getApplicationContext());


        //Setup the List Posts Query
        listPostsQuery = ListPostsQuery.builder().build();
        GraphQLCall.Callback listPostsQueryCallback = new GraphQLCall.Callback<ListPostsQuery.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<ListPostsQuery.Data> response) {
                if (response == null  || response.data() == null ||
                        response.data().listPosts() == null ||
                        response.data().listPosts() == null ) {
                    Log.d(TAG, "List Posts returned with no data");
                    return;
                }

                Log.d(TAG, "listPostsQuery returned data. Iterating over the data");
                display.runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       for (ListPostsQuery.ListPost p: response.data().listPosts()) {
                           //Populate the allPosts map with the posts returned by the query.
                           //The allPosts map is used by the recycler view.

                           if ( allPosts.get(p.id()) == null )  {
                               mPostIDs.add(0, p.id());
                           }
                           allPosts.put(p.id(), new ListPostsQuery.ListPost("Post",
                                   p.id(), p.author(), p.title(), p.content(),
                                   p.url(),p.ups(),p.downs(),
                                   p.createdDate(),
                                   p.aws_ds()));
                       }
                       //Trigger the view to refresh by calling notifyDataSetChanged method
                       notifyDataSetChanged();
                   }
               });
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "ListPostsQuery failed with [" + e.getLocalizedMessage() + "]");
            }
        };
        

        //Setup Delta Post Subscription to get notified when a change (add, update, delete) happens on a Post
        OnDeltaPostSubscription onDeltaPostSubscription =
                OnDeltaPostSubscription.builder().build();
        AppSyncSubscriptionCall.Callback onDeltaPostSubscriptionCallback =
                new AppSyncSubscriptionCall.Callback<OnDeltaPostSubscription.Data>() {
            @Override
            public void onResponse(@Nonnull Response<OnDeltaPostSubscription.Data> response) {
                Log.d(TAG, "Delta on a post received via subscription.");
                if (response == null  || response.data() == null
                        || response.data().onDeltaPost() == null ) {
                    Log.d(TAG, "Delta was null!");
                    return;
                }

                Log.d(TAG, "Updating post to display");
                final OnDeltaPostSubscription.OnDeltaPost p = response.data().onDeltaPost();

                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Delete the Post if the aws_ds has the DELETE tag
                        if ( DeltaAction.DELETE == p.aws_ds()) {
                            mPostIDs.remove(p.id());
                            allPosts.remove(p.id());
                        }
                        // Add/Update the post otherwise
                        else {
                            if (allPosts.get(p.id()) == null) {
                                mPostIDs.add(0, p.id());
                            }
                            allPosts.put(p.id(), new ListPostsQuery.ListPost("Post",
                                    p.id(), p.author(), p.title(), p.content(),
                                    p.url(),p.ups(), p.downs(),
                                    p.createdDate(),
                                    p.aws_ds()));
                        }

                        //Update the baseQuery Cache
                        updateListPostsQueryCache();

                        //Trigger the view to refresh by calling notifyDataSetChanged method
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


        //Setup the Delta Query to get changes to posts incrementally
        Query listPostsDeltaQuery = ListPostsDeltaQuery.builder().build();

        GraphQLCall.Callback listPostsDeltaQueryCallback = new GraphQLCall.Callback<ListPostsDeltaQuery.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<ListPostsDeltaQuery.Data> response) {
                if (response == null  || response.data() == null || response.data().listPostsDelta() == null ) {
                    Log.d(TAG, "listPostsDelta returned with no data");
                    return;
                }

                //Add to the ListPostsQuery Cache
                Log.d(TAG, "listPostsDelta returned data. Iterating...");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (ListPostsDeltaQuery.ListPostsDeltum p: response.data().listPostsDelta() ) {
                            // Delete the Post if the aws_ds has the DELETE tag
                            if ( DeltaAction.DELETE == p.aws_ds()) {
                                mPostIDs.remove(p.id());
                                allPosts.remove(p.id());
                                Log.v(TAG, "Got Post [" + p.id() + "] with aws_ds set to DELETE");
                                continue;
                            }

                            Log.v(TAG, "Got Post [" + p.id() + "] with aws_ds not set to DELETE");
                            // Add or Update the post otherwise
                            if (allPosts.get(p.id()) == null ) {
                                mPostIDs.add(0, p.id());
                            }
                            allPosts.put(p.id(), new ListPostsQuery.ListPost("Post",
                                    p.id(), p.author(), p.title(), p.content(),
                                    p.url(),p.ups(), p.downs(),
                                    p.createdDate(),
                                    p.aws_ds()));
                        }

                        //Update the baseQuery Cache
                        updateListPostsQueryCache();

                        //Trigger the view to refresh by calling notifyDataSetChanged method
                        notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                Log.e(TAG, "listPostsDelta Query failed with [" + e.getLocalizedMessage() + "]");
            }
        };

        client.sync(listPostsQuery, listPostsQueryCallback,    onDeltaPostSubscription , onDeltaPostSubscriptionCallback, listPostsDeltaQuery, listPostsDeltaQueryCallback, 20 * 60 );


    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.post, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder,  int pos) {
        final int position = pos;
        if ( mPostIDs.size() < position ) {
            return;
        }

        final ListPostsQuery.ListPost  item  = allPosts.get(mPostIDs.get(position));
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

        holder.mDeleteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(display, "Deleting post", Toast.LENGTH_SHORT).show();
                deletePost(view, position, item);
            }
        });
    }



    private void deletePost(final View view, final int position, final ListPostsQuery.ListPost p) {
        //Execute Mutation

        final DeletePostMutation deletePostMutation = DeletePostMutation.builder()
                .id(p.id())
                .build();
        ClientFactory.getInstance(view.getContext()).mutate(deletePostMutation).enqueue(new GraphQLCall.Callback<DeletePostMutation.Data>() {
            @Override
            public void onResponse(@Nonnull final Response<DeletePostMutation.Data> response) {
                Log.d(TAG, "Deleted successfully");
                display.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(display, "Deleted!", Toast.LENGTH_SHORT).show();
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


    private void upVotePost(final View view, final int position, final ListPostsQuery.ListPost p) {

        //Execute Mutation
        final UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(p.id())
                        .title(p.title())
                        .author(p.author())
                        .content(p.content())
                        .ups(p.ups() + 1)
                        .downs(p.downs())
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
                allPosts.put(p.id(), new ListPostsQuery.ListPost("Post",
                        p.id(), p.author(), p.title(), p.content(),
                        p.url(), p.ups() + 1, p.downs(), p.createdDate(),
                        p.aws_ds()));
                updateListPostsQueryCache();
                notifyDataSetChanged();
            }
        });

    }

    private void downVotePost(final View view, final int position, final ListPostsQuery.ListPost p) {
        //Execute Mutation
        UpdatePostMutation updatePostMutation = UpdatePostMutation.builder()
                .input(UpdatePostInput.builder()
                        .id(p.id())
                        .title(p.title())
                        .author(p.author())
                        .content(p.content())
                        .ups(p.ups())
                        .downs(p.downs()+1)
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
                allPosts.put(p.id(), new ListPostsQuery.ListPost("Post",
                        p.id(), p.author(), p.title(), p.content(),
                        p.url(), p.ups(), p.downs() + 1, p.createdDate(),
                        p.aws_ds()));
                updateListPostsQueryCache();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPostIDs.size();
    }

    private void updateListPostsQueryCache() {
        List<ListPostsQuery.ListPost> items = new ArrayList<>();
        items.addAll(allPosts.values());

        ListPostsQuery.Data data = new ListPostsQuery.Data(items);
        ClientFactory.getInstance(display.getApplicationContext()).getStore()
                .write(listPostsQuery, data).enqueue(null);
    }

}
