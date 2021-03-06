package com.example.hashpotatoesv20.Profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.hashpotatoesv20.Models.Comment;
import com.example.hashpotatoesv20.Models.Like;
import com.example.hashpotatoesv20.Models.Post;
import com.example.hashpotatoesv20.Models.Tag;
import com.example.hashpotatoesv20.Models.User;
import com.example.hashpotatoesv20.Models.UserAccountSettings;
import com.example.hashpotatoesv20.Models.UserSettings;
import com.example.hashpotatoesv20.R;
import com.example.hashpotatoesv20.Utils.BottomNavigationViewHelper;
import com.example.hashpotatoesv20.Utils.FirebaseMethods;
import com.example.hashpotatoesv20.Utils.FollowingFragment;
import com.example.hashpotatoesv20.Utils.MainfeedListAdapter;
import com.example.hashpotatoesv20.Utils.UniversalImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int ACTIVITY_NUM = 3;

    public interface onListPostSelectedListener {
        void onPostSelected(Post post, int activity_number);
    }
    onListPostSelectedListener mOnListPostSelectedListener;

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    private TextView mPosts, mHashtags, mDisplayName, mYear, mMajor, mUsername, mWebsite, mDescription, tvYear, tvHash;
    private ProgressBar mProgressBar;
    private CircleImageView mProfilePhoto;
    private Toolbar toolbar;
    private ImageView profileMenu;
    private BottomNavigationViewEx bottomNavigationView;
    private ListView listView;
    private SwipeRefreshLayout pullToRefresh;

    private Context mContext;
    private UserAccountSettings mUserAccountSettings;
    private int mPostsCount = 0;
    private int mTagsCount = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_profile, container,false);
        mDisplayName = (TextView) view.findViewById(R.id.display_name);
        mUsername = (TextView) view.findViewById(R.id.username);
        mYear = (TextView) view.findViewById(R.id.profile_year);
        mMajor = (TextView) view.findViewById(R.id.profile_major);
        mProfilePhoto = (CircleImageView) view.findViewById(R.id.profilePhoto);
        mPosts = (TextView) view.findViewById(R.id.tvPost);
        mHashtags = (TextView) view.findViewById(R.id.tvHash);
        tvHash = (TextView) view.findViewById(R.id.textHash);
        mProgressBar = (ProgressBar) view.findViewById(R.id.profileProgressBar);
        toolbar = (Toolbar) view.findViewById(R.id.profileToolBar);
        profileMenu = (ImageView) view.findViewById(R.id.profileMenu);
        bottomNavigationView = (BottomNavigationViewEx) view.findViewById(R.id.bottomNavViewBar);
        mWebsite = (TextView) view.findViewById(R.id.profile_website);
        mDescription = (TextView) view.findViewById(R.id.profile_description);
        tvYear = (TextView) view.findViewById(R.id.tv_year);
        pullToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.pullToRefresh);
        mContext = getActivity();
        mFirebaseMethods = new FirebaseMethods(getActivity());
        listView = (ListView) view.findViewById(R.id.listView);

        pullToRefresh.setDistanceToTriggerSync(20);

        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());

        setupBottomNavigationView();
        setupToolbar();

        setupFirebaseAuth();
        setupListView();

        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setupListView();
                pullToRefresh.setRefreshing(false);
            }
        });

        TextView editProfile = (TextView) view.findViewById(R.id.textEditProfile);
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to: " + mContext.getString(R.string.edit_profile_fragment));
                Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                intent.putExtra(mContext.getString(R.string.calling_activity), mContext.getString(R.string.profile_activity));
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        TextView createTag = (TextView) view.findViewById(R.id.textCreate);
        createTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to CreateTagActivity");
                Intent intent = new Intent(getActivity(), CreateTagActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        mHashtags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to following fragment");
                FollowingFragment fragment = new FollowingFragment();

                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(getString(R.string.following_fragment));
                transaction.commit();
            }
        });

        tvHash.setVisibility(View.GONE);

        Log.d(TAG, "onCreateView: Started.");

        return view;
    }

    @Override
    public void onAttach(Context context) {
        try {
            mOnListPostSelectedListener = (onListPostSelectedListener) getActivity();
            //mOnAdapterItemClickListener = (ProfileListAdapter.OnAdapterItemClickListener) getActivity();
        }
        catch (ClassCastException e) {
            Log.e(TAG, "onAttach: ClassCastException" + e.getMessage());
        }
        super.onAttach(context);
    }

    private void getCounts() {
        mPostsCount = 0;
        mTagsCount = 0;

        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference();
        Query query1 = reference1.child(mContext.getString(R.string.dbname_user_posts))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found post: " + ds.getValue());
                    mPostsCount++;
                }
                mPosts.setText(String.valueOf(mPostsCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        DatabaseReference reference2 = FirebaseDatabase.getInstance().getReference();
        Query query2 = reference2.child(mContext.getString(R.string.dbname_user_following))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found post: " + ds.getValue());
                    mTagsCount++;
                }
                if (mTagsCount == 1) {
                    mHashtags.setText(String.valueOf(mTagsCount) + " Hashtag");
                }
                else {
                    mHashtags.setText(String.valueOf(mTagsCount) + " Hashtags");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void setupListView() {
        Log.d(TAG, "setupListView: Setting up list of user posts.");
//        final ArrayList<Post> posts = new ArrayList<>();
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            final Query query = reference
                    .child(mContext.getString(R.string.dbname_user_posts))
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final ArrayList<Post> posts = new ArrayList<>();
                    try {
                        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                            Post post = new Post();
                            Map<String,Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();
                            Log.d(TAG, "onDataChange: snapshot profile: " + objectMap.get(mContext.getString(R.string.field_discussion)).toString());
                            post.setDiscussion(objectMap.get(mContext.getString(R.string.field_discussion)).toString());
                            post.setDate_created(objectMap.get(mContext.getString(R.string.field_date_created)).toString());
                            post.setUser_id(objectMap.get(mContext.getString(R.string.field_user_id)).toString());
                            post.setPost_id(objectMap.get(mContext.getString(R.string.field_post_id)).toString());
                            post.setTags(objectMap.get(mContext.getString(R.string.field_tags)).toString());
                            post.setAnonymity(objectMap.get(mContext.getString(R.string.field_anonymity)).toString());

                            ArrayList<String> tagIDList = new ArrayList<>();
                            for (DataSnapshot dataSnapshot1 : singleSnapshot
                                    .child(mContext.getString(R.string.field_tag_list)).getChildren()) {
                                Log.d(TAG, "onDataChange: post taglist" + dataSnapshot1.getValue());
                                tagIDList.add(dataSnapshot1.getValue().toString());
                            }

                            List<Like> likesList = new ArrayList<Like>();
                            for (DataSnapshot dSnapshot : singleSnapshot
                                    .child(mContext.getString(R.string.field_likes)).getChildren()){
                                Like like = new Like();
                                like.setUser_id(dSnapshot.getValue(Like.class).getUser_id());
                                likesList.add(like);
                            }

                            List<Comment> commentList = new ArrayList<>();
                            for (DataSnapshot dataSnapshot1 :
                                    singleSnapshot.child(mContext.getString(R.string.field_comments)).getChildren()) {
                                Comment comment = new Comment();
                                comment.setUser_id(dataSnapshot1.getValue(Comment.class).getUser_id());
                                comment.setComment(dataSnapshot1.getValue(Comment.class).getComment());
                                comment.setDate_created(dataSnapshot1.getValue(Comment.class).getDate_created());
                            }

                            post.setTag_list(tagIDList);
                            post.setComments(commentList);
                            post.setLikes(likesList);
                            posts.add(post);
                        }
                    }
                    catch (NullPointerException e) {
                        Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage() );
                    }

                    Collections.sort(posts, new Comparator<Post>() {
                        @Override
                        public int compare(Post o1, Post o2) {
                            return o2.getDate_created().compareTo(o1.getDate_created());
                        }
                    });

                    MainfeedListAdapter adapter = new MainfeedListAdapter(mContext, R.layout.layout_mainfeed_listitem, posts);

                    listView.setAdapter(adapter);
                    setListViewHeightBasedOnChildren(listView);

                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mOnListPostSelectedListener.onPostSelected(posts.get(position), ACTIVITY_NUM);
                        }
                    });

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: query cancelled.");
                }
            });
    }

    private static void setListViewHeightBasedOnChildren (ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) return;
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(),
                View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) view.setLayoutParams(new
                    ViewGroup.LayoutParams(desiredWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight() + 20;
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();

        int width = listAdapter.getCount() - 1;

        params.height = totalHeight + (listView.getDividerHeight() * (width));

        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    /**
     * Returns a string representing the number of days aho the post was made
     * @return
     */
    private String getTimestampDifference(String postTimestamp) {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;

        try {
            timestamp = sdf.parse(postTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60)));

            int tsDiff = Integer.parseInt(difference);
            String time = "";
            if (tsDiff == 0) { //less than one minute
                time = "A FEW SECONDS AGO";
                return time;
            }
            else if (tsDiff < 60) { //less than one hour
                if (tsDiff == 1) {
                    time = tsDiff + " MIN AGO";
                }
                else {
                    time = tsDiff + " MINS AGO";
                }
                return time;
            }
            else if (tsDiff < 1440) { //less than one day
                tsDiff = tsDiff / 60;
                if (tsDiff == 1) {
                    time = tsDiff + " HOUR AGO";
                }
                else {
                    time = tsDiff + " HOURS AGO";
                }
                return time;
            }
            else {
                tsDiff = tsDiff / 60/ 24;
                if (tsDiff == 1) {
                    time = tsDiff + " DAY AGO";
                }
                else {
                    time = tsDiff + " DAYS AGO";
                }
                return time;
            }
        }
        catch (ParseException e) {
            Log.e(TAG, "getTimestampDifference: ParseException: " + e.getMessage());
            difference = "0";
        }
        return difference;
    }

    private void setProfileWidgets(UserSettings userSettings) {
        UserAccountSettings settings = userSettings.getSettings();

        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, null, "");
        mDisplayName.setText(settings.getDisplay_name());
        mUsername.setText(settings.getUsername());
        mWebsite.setText(settings.getWebsite());
        mDescription.setText(settings.getDescription());
        mYear.setText("Year " + settings.getYear());
        mMajor.setText(String.valueOf(settings.getMajor()));

        if (settings.getWebsite().isEmpty()) {
            mWebsite.setVisibility(View.GONE);
        }
        if (settings.getYear().isEmpty()) {
            mYear.setVisibility(View.GONE);
            tvYear.setVisibility(View.GONE);
        }
        if (settings.getMajor().isEmpty()) {
            mMajor.setVisibility(View.GONE);
        }
        if (settings.getDescription().isEmpty()) {
            mDescription.setVisibility(View.GONE);
        }

        mProgressBar.setVisibility(View.GONE);
        getCounts();
    }

    /**
     * Responsible for setting up the profile Toolbar
     */
    private void setupToolbar() {
        ((ProfileActivity) getActivity()).setSupportActionBar(toolbar);

        profileMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to account settings.");
                Intent intent = new Intent(mContext, AccountSettingsActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });
    }

    /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView() {
        Log.d(TAG, "setupBottomNavigationView: setting up bottom navigation view");

        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationView);
        BottomNavigationViewHelper.enableNavigation(mContext, getActivity(),bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }
    /*
    ---------------------------------------firebase------------------------------------------------
     */
    /**
     * Setup the firebase auth object
     */
    private void setupFirebaseAuth() {
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    //User is signed in
                    Log.d(TAG, "onAuthStateChanged: signed_in:" + user.getUid());
                }
                else {
                    //User is signed out
                    Log.d(TAG, "onAuthStateChanged: signed out");
                }
            }
        };

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

}
