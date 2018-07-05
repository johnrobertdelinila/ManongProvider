/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.johnrobert.manongprovider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageCustomerActivity extends AppCompatActivity {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
        }
    }

    private static final String TAG = "MessageCustomerActivity";
    private static final int REQUEST_IMAGE = 2;
    public static final String ANONYMOUS = "Anonymous";
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private String mUsername;
    private String mPhotoUrl;

    private MaterialButton mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    public ImageView mAddMessageImageView;
    private AdView mAdView;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

    private String messagelinkKey;
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference messagesRef = rootRef.child("Messages");
    private boolean doneSettingUp;

    private String providerId;
    private String providerName;
    private String providerPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isSlideTransition = getIntent().getBooleanExtra("isSlideTransition", true);
        if (isSlideTransition) {
            setUpEnterTransition();
            setContentView(R.layout.activity_message_customer_slide_transition);
        }else {
            setContentView(R.layout.activity_message_customer);
        }

        messagelinkKey = getIntent().getStringExtra("messageLinkKey");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        providerPhoto = getIntent().getStringExtra("providerPhoto");
        doneSettingUp = false;

        if (mFirebaseUser != null) {
            // Getting the name from Firebase User
            mUsername = mFirebaseUser.getDisplayName();
            // Checking if the username is null,
            // Maybe get the name from the provider data.
            if (mUsername != null && mUsername.trim().equalsIgnoreCase("") || mUsername == null) {
                for (UserInfo userInfo : mFirebaseUser.getProviderData()) {
                    String displayName = userInfo.getDisplayName();
                    if (displayName != null) {
                        mUsername = displayName;
                    }
                }
            }
            // Checking if the display name is still "" or null.
            if (mUsername != null && mUsername.trim().equalsIgnoreCase("") || mUsername == null) {
                // Just name it Anonymous.
                mUsername = ANONYMOUS;
                // Trying to get the User Display Name using the Cloud Function.
                getUserRecord(mFirebaseUser.getUid())
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Exception e = task.getException();
                                if (e instanceof FirebaseFunctionsException) {
                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                    Object details = ffe.getDetails();
                                    Toast.makeText(this, "Error: " + String.valueOf(details), Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }
                            String displayName = (String) task.getResult().get("displayName");
                            if (displayName != null && displayName.trim().equals("") || displayName == null) {
                                displayName = ANONYMOUS;
                            }
                            mUsername = displayName;
                            if (!mUsername.equalsIgnoreCase(ANONYMOUS)) {
                                // Update all the names in the chat
                                updateAllUserName(mUsername);
                            }
                        });
            }

            // Getting the Photo Url from Firebase User
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
            // If the photo is still null.
            if (mPhotoUrl != null && mPhotoUrl.trim().equalsIgnoreCase("") || mPhotoUrl == null) {
                for (UserInfo userInfo : mFirebaseUser.getProviderData()) {
                    Uri photo = userInfo.getPhotoUrl();
                    if (photo != null) {
                        mPhotoUrl = photo.toString();
                    }
                }
            }

        }

        mProgressBar = findViewById(R.id.progressBar);
        mMessageRecyclerView = findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("Messages")) {
                    rootRef.removeEventListener(this);

                    messagesRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.hasChild(messagelinkKey) && !doneSettingUp) {
                                setUpChatMessage();
                            }else {
                                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                            }
                            // Remove the listener because we now know that their chat message already exist.
                            if (doneSettingUp) {
                                updateUserDisplayName(providerName, providerPhoto, providerId);
                                updateUserDisplayName(mUsername, mPhotoUrl, mFirebaseUser.getUid());
                                messagesRef.removeEventListener(this);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(MessageCustomerActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }else {
                    mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MessageCustomerActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize and request AdMob ad.
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mMessageEditText = findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mAddMessageImageView = findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

        mSendButton = findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(view -> {
            ManongMessage manongMessage = new ManongMessage(mMessageEditText.getText().toString(), mUsername,
                    mPhotoUrl, null, mFirebaseUser.getUid(), ServerValue.TIMESTAMP);
            messagesRef.child(messagelinkKey).push().setValue(manongMessage);
            mMessageEditText.setText("");
        });
    }

    private void setUpEnterTransition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            Slide slide = new Slide(Gravity.END);
            slide.excludeTarget(android.R.id.statusBarBackground, true);
            slide.excludeTarget(android.R.id.navigationBarBackground, true);
            getWindow().setEnterTransition(slide);
        }
    }

    private void updateAllUserName(String newName) {
        Query query = messagesRef.child(messagelinkKey).orderByChild("uid").equalTo(mFirebaseUser.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    messagesRef.child(messagelinkKey).child(Objects.requireNonNull(childSnapshot.getKey())).child("name").setValue(newName)
                            .addOnFailureListener(e -> Toast.makeText(MessageCustomerActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MessageCustomerActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUpChatMessage() {
        FirebaseRecyclerOptions<ManongMessage> options =
                new FirebaseRecyclerOptions.Builder<ManongMessage>()
                        .setQuery(messagesRef.child(messagelinkKey), ManongMessage.class)
                        .build();

        mFirebaseAdapter = new FirebaseRecyclerAdapter<ManongMessage, MessageViewHolder>(options) {

            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull ManongMessage message) {

                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (message.getText() != null) {
                    holder.messageTextView.setText(message.getText());
                    holder.messageTextView.setVisibility(TextView.VISIBLE);
                    holder.messageImageView.setVisibility(ImageView.GONE);
                } else if (message.getImageUrl() != null) {
                    String imageUrl = message.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                task -> {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(holder.messageImageView.getContext())
                                                .load(downloadUrl)
                                                .into(holder.messageImageView);
                                    } else {
                                        Log.w(TAG, "Getting download url was not successful.",
                                                task.getException());
                                    }
                                });
                    } else {
                        Glide.with(holder.messageImageView.getContext())
                                .load(message.getImageUrl())
                                .into(holder.messageImageView);
                    }
                    holder.messageImageView.setVisibility(ImageView.VISIBLE);
                    holder.messageTextView.setVisibility(TextView.GONE);
                }

                holder.messengerTextView.setText(message.getName());
                if (message.getPhotoUrl() == null) {
                    holder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MessageCustomerActivity.this,
                            R.mipmap.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MessageCustomerActivity.this)
                            .load(message.getPhotoUrl())
                            .into(holder.messengerImageView);
                }
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mFirebaseAdapter.startListening();
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        doneSettingUp = true;
    }

    private void updateUserDisplayName(String name, String photo, String uid) {
        if (name != null && photo != null) {
            updateMessageDisplayName(name, photo, uid);
        }else {
            getUserRecord(uid)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                Object details = ffe.getDetails();
                                Toast.makeText(MessageCustomerActivity.this, "Error: " + String.valueOf(details), Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                        String displayName = (String) task.getResult().get("displayName");
                        String photoURL = (String) task.getResult().get("photoURL");
                        if (photoURL != null) {
                            if (photoURL.startsWith("https://graph.facebook.com")) {
                                photoURL = photoURL.concat("?height=100");
                            }
                        }
                        updateMessageDisplayName(displayName, photoURL, uid);
                    });
        }
    }

    private void updateMessageDisplayName(String providerNewDisplayname, String photoURL, String uid) {
        Query query = messagesRef.child(messagelinkKey).orderByChild("uid").equalTo(uid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    ManongMessage providerMessage = childSnapshot.getValue(ManongMessage.class);
                    if (providerMessage != null) {
                        if (providerMessage.getUid().equals(uid)) {
                            String key = childSnapshot.getKey();
                            if (key != null) {
                                if (providerNewDisplayname != null) {
                                    HashMap<String, Object> newName = new HashMap<>();
                                    newName.put("name", providerNewDisplayname);
                                    messagesRef.child(messagelinkKey).child(key).updateChildren(newName);
                                }
                                if (photoURL != null) {
                                    HashMap<String, Object> newPhoto = new HashMap<>();
                                    newPhoto.put("photoUrl", photoURL);
                                    messagesRef.child(messagelinkKey).child(key).updateChildren(newPhoto);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MessageCustomerActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.stopListening();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        if (mAdView != null) {
            mAdView.pause();
        }
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.stopListening();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    assert uri != null;
                    Log.d(TAG, "Uri: " + uri.toString());

                    ManongMessage tempMessage = new ManongMessage(null, mUsername, mPhotoUrl,
                            LOADING_IMAGE_URL, mFirebaseUser.getUid(), ServerValue.TIMESTAMP);
                    messagesRef.child(messagelinkKey).push()
                            .setValue(tempMessage, (databaseError, databaseReference) -> {
                                if (databaseError == null) {
                                    String key = databaseReference.getKey();
                                    assert key != null;
                                    StorageReference storageReference =
                                            FirebaseStorage.getInstance()
                                                    .getReference(mFirebaseUser.getUid())
                                                    .child(key)
                                                    .child(uri.getLastPathSegment());

                                    putImageInStorage(storageReference, uri, key);
                                } else {
                                    Log.w(TAG, "Unable to write message to database.",
                                            databaseError.toException());
                                }
                            });
                }
            }
        }
    }

    private void putImageInStorage(final StorageReference storageReference, Uri uri, final String key) {

        UploadTask uploadTask = storageReference.putFile(uri);

        uploadTask.addOnCompleteListener(MessageCustomerActivity.this,
                task -> {
                    if (!task.isSuccessful() || !task.isComplete()) {
                        Log.w(TAG, "Image upload task was not successful.",
                                task.getException());
                    }
                });

        uploadTask
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return storageReference.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ManongMessage manongMessage =
                                new ManongMessage(null, mUsername, mPhotoUrl, task.getResult().toString(), mFirebaseUser.getUid(), ServerValue.TIMESTAMP);
                        messagesRef.child(messagelinkKey).child(key)
                                .setValue(manongMessage);
                    }
                });

    }

    private Task<Map<String, Object>> getUserRecord(String uid) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        // Call callable function from Firebase Functions
        return mFunctions.getHttpsCallable("getUserRecord").call(data)
                .continueWith(task -> {
                    Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
//                    return (String) result.get("displayName");
                    return result;
                });

    }

}
