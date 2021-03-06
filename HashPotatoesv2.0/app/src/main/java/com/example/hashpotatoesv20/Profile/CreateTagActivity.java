package com.example.hashpotatoesv20.Profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.hashpotatoesv20.R;
import com.example.hashpotatoesv20.Utils.FirebaseMethods;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class CreateTagActivity extends AppCompatActivity {
    private static final String TAG = "CreateTagActivity";

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;

    //widgets
    private EditText mTagName, mTagDescription;
    private SwitchCompat mPrivacy;
    private TextView tvPrivacy, tvError;
    private RelativeLayout mParent;

    private Context mContext = CreateTagActivity.this;

    @Nullable
    //@Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_createtag, container,false);

        return view;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createtag);
        Log.d(TAG, "onCreate: Activity CreateTag started.");
        mTagName = (EditText) findViewById(R.id.tag_name);
        mTagDescription = (EditText) findViewById(R.id.tagDescription);
        tvPrivacy = (TextView) findViewById(R.id.tvPrivacy);
        tvError = (TextView) findViewById(R.id.error_msg);
        mPrivacy = (SwitchCompat) findViewById(R.id.privacy);
        mParent = (RelativeLayout) findViewById(R.id.parent_container);
        mFirebaseMethods = new FirebaseMethods(CreateTagActivity.this);

        setupFirebaseAuth();
        setupUI(mParent);

        mPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPrivacy.isChecked()) {
                    Log.d(TAG, "onClick: Privacy switch checked: Private");
                    tvPrivacy.setText("Private");
                }
                else {
                    Log.d(TAG, "onClick: Privacy switch checked: Public");
                    tvPrivacy.setText("Public");
                }
            }
        });

        Button cancel = (Button) findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: closing activity, go back to previous screen");
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }
        });

        Button create = (Button) findViewById(R.id.btn_create);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: creating tag.");
                //upload post to firebase
                Toast.makeText(CreateTagActivity.this, "Attempting to create tag", Toast.LENGTH_SHORT).show();

                //check if tag name already exists
                String tag_name = mTagName.getText().toString();
                if (tag_name.charAt(0) == '#') {
                    tag_name = tag_name.substring(1);
                }
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                Query query = reference.child(getString(R.string.dbname_tags))
                        .orderByChild(getString(R.string.field_tag_name))
                        .equalTo(tag_name);

                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.d(TAG, "onDataChange: ds: " + dataSnapshot.getValue());

                        if (dataSnapshot.exists()) {
                            Log.d(TAG, "onDataChange: Tag name exists.");
                            //Toast.makeText(mContext, "Tag name exists. Please try again.", Toast.LENGTH_SHORT).show();
                            tvError.setVisibility(View.VISIBLE);
                        }
                        else {
                            String tag_desc = mTagDescription.getText().toString();
                            String privacy = tvPrivacy.getText().toString();
                            String tag_name = mTagName.getText().toString();
                            Log.d(TAG, "onDataChange: tag name: " + tag_name + "\ntag desc: " + tag_desc + "\nprivacy: " + privacy);

                            try {
                                mFirebaseMethods.addTagToDatabase(tag_name, tag_desc, privacy);
                                Intent intent = new Intent(mContext, ProfileActivity.class);
                                mContext.startActivity(intent);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            }
                            catch (NullPointerException e) {
                                Log.e(TAG, "onDataChange: NullPointerException: " + e.getLocalizedMessage() );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    //hide keyboard if user touches view
    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus() != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);
        }
    }

    /*
    ---------------------------------------firebase------------------------------------------------
    */

    /**
     * Setup the firebase auth object
     **/
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
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}
