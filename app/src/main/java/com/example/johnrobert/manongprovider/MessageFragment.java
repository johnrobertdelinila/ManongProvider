package com.example.johnrobert.manongprovider;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MessageFragment extends Fragment {

    private Activity activity;
    private Context context;
    private int animatedRow = -1;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textNoData;

    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = mDatabase.getReference();
    private DatabaseReference providerRef = rootRef.child("Providers");
    private DatabaseReference messagesRef = rootRef.child("Messages");
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseRecyclerAdapter firebaseAdapter;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

    public MessageFragment() {
        // Required empty public constructor
    }

    public class CustomerContact {
        private String providerId, messageLinkKey;

        public CustomerContact() {}

        public CustomerContact(String providerId, String messageLinkKey) {
            this.providerId = providerId;
            this.messageLinkKey = messageLinkKey;
        }

        public String getMessageLinkKey() {
            return messageLinkKey;
        }

        public void setMessageLinkKey(String messageLinkKey) {
            this.messageLinkKey = messageLinkKey;
        }

        public String getProviderId() {
            return providerId;
        }

        public void setProviderId(String providerId) {
            this.providerId = providerId;
        }
    }

    public class CustomerViewHolder extends RecyclerView.ViewHolder{

        CircleImageView profile_picture;
        TextView text_last_date, text_last_message, text_provider_name;
        CardView temp_image_view;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            profile_picture = itemView.findViewById(R.id.image_profile_picture);
            text_last_date = itemView.findViewById(R.id.text_last_date);
            text_last_message = itemView.findViewById(R.id.text_last_message);
            text_provider_name = itemView.findViewById(R.id.text_provider_name);
            temp_image_view = itemView.findViewById(R.id.temp_image_view);
        }

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        activity = getActivity();
        context = getContext();

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        progressBar = view.findViewById(R.id.progress_bar);
        textNoData = view.findViewById(R.id.text_no_data);

        if (user != null) {
            // Database Reference of all the provider's Customer list.
            providerRef.child(user.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("customers")) {
                        DatabaseReference customerContactRef = providerRef.child(user.getUid()).child("customers");
                        setUpFirebaseRecyclerView(customerContactRef);
                    }else {
                        // TODO: Update UI.
                        // No request is available.
                        progressBar.setVisibility(ProgressBar.GONE);
                        textNoData.setVisibility(TextView.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }

        return view;
    }

    private void setUpFirebaseRecyclerView(DatabaseReference customerContactRef) {

        SnapshotParser<CustomerContact> customerParser = snapshot -> {
            CustomerContact customerContact = new CustomerContact();
            String messageLink = snapshot.getValue(String.class);
            customerContact.setProviderId(snapshot.getKey());
            customerContact.setMessageLinkKey(messageLink);
            return customerContact;
        };

        FirebaseRecyclerOptions<CustomerContact> options = new FirebaseRecyclerOptions.Builder<CustomerContact>()
                .setQuery(customerContactRef, customerParser)
                .build();

        firebaseAdapter = new FirebaseRecyclerAdapter<CustomerContact, CustomerViewHolder>(options) {
            @NonNull
            @Override
            public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view1 = LayoutInflater.from(context).inflate(R.layout.list_item_provider, viewGroup, false);
                return new CustomerViewHolder(view1);
            }

            @Override
            protected void onBindViewHolder(@NonNull CustomerViewHolder holder, int position, @NonNull CustomerContact customer) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                Intent intent = new Intent(context, MessageCustomerActivity.class);
                intent.putExtra("messageLinkKey", customer.getMessageLinkKey());
                intent.putExtra("isSlideTransition", false);
                intent.putExtra("providerId", customer.getProviderId());

                // TODO: Retrieve the Provider Information

                checkIfMessageExist(customer.getProviderId(), customer.getMessageLinkKey(), holder.text_last_message, holder.text_last_date, holder.text_provider_name,
                        holder.profile_picture, holder.temp_image_view, intent);

                if (position > animatedRow) {
                    animatedRow = position;
                    long animationDelay = 200L + holder.getAdapterPosition() * 25;

                    holder.itemView.setAlpha(0);
                    holder.itemView.setTranslationY(ScreenUtil.dp2px(8, holder.itemView.getContext()));

                    holder.itemView.animate()
                            .alpha(1)
                            .translationY(0)
                            .setDuration(200)
                            .setInterpolator(new LinearOutSlowInInterpolator())
                            .setStartDelay(animationDelay)
                            .start();
                }

                holder.itemView.setOnClickListener(view -> {
                    String elementName = getString(R.string.transition_name_navigational_transition);
                    ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, elementName);
                    startActivity(intent, activityOptionsCompat.toBundle());
                });

            }
        };

        recyclerView.setAdapter(firebaseAdapter);
        firebaseAdapter.startListening();
    }

    private void setPhotoAndDisplayName(String providerId, CircleImageView profile_picture, TextView text_provider_name,
                                        CardView temp_image, Intent intent) {
        getUserRecord(providerId)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            FirebaseFunctionsException.Code code = ffe.getCode();
                            Object details = ffe.getDetails();
                            Toast.makeText(activity, "Error: " + String.valueOf(details), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    String displayName = (String) task.getResult().get("displayName");
                    String photoURL = (String) task.getResult().get("photoURL");

                    if (activity != null) {
                        if (photoURL != null) {
                            if (photoURL.startsWith("https://graph.facebook.com")) {
                                photoURL = photoURL.concat("?height=100");
                            }
                            Glide.with(activity)
                                    .load(photoURL)
                                    .into(profile_picture);
                        }
                        temp_image.setVisibility(CardView.GONE);
                        profile_picture.setVisibility(CircleImageView.VISIBLE);
                        if (displayName != null) {
                            text_provider_name.setText(displayName);
                            intent.putExtra("providerName", displayName);
                        }else {
                            text_provider_name.setText("Anonymous");
                        }
                    }
                });
    }

    private void checkIfMessageExist(String getProviderId, String messageLinkKey, TextView text_last_message, TextView text_last_date, TextView text_provider_name,
                                     CircleImageView profile_picture, CardView temp_image, Intent intent) {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(messageLinkKey)) {
                    messagesRef.removeEventListener(this);
                    setPhotoAndDisplayName(getProviderId, profile_picture, text_provider_name, temp_image, intent);
                    setProviderInformation(getProviderId, temp_image, messageLinkKey, text_last_message, text_last_date, text_provider_name, profile_picture);
                }else {
                    setPhotoAndDisplayName(getProviderId, profile_picture, text_provider_name, temp_image, intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setProviderInformation(String providerId, CardView temp_image, String messageLinkKey, TextView text_last_message, TextView text_last_date, TextView text_provider_name, CircleImageView profile_picture) {
        messagesRef.child(messageLinkKey).limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                    ManongMessage lastMessage = childSnapshot.getValue(ManongMessage.class);
                    if (lastMessage != null) {
                        // TODO: Limit the last text character
                        SimpleDateFormat sfd = new SimpleDateFormat("d MMM y, h:mm a", Locale.getDefault());
                        String dateTime = sfd.format(new Date((Long) lastMessage.getTimestamp()));
                        text_last_message.setText(lastMessage.getText());
                        text_last_date.setText(dateTime);

                        if (lastMessage.getUid().equalsIgnoreCase(providerId)) {
                            if (lastMessage.getName() != null && !lastMessage.getName().trim().equalsIgnoreCase("")) {
                                text_provider_name.setText(lastMessage.getName());
                            }
                            if (lastMessage.getPhotoUrl() != null && !lastMessage.getPhotoUrl().trim().equalsIgnoreCase("") && !lastMessage.getPhotoUrl().trim().equalsIgnoreCase("null")) {
                                String photoURL = lastMessage.getPhotoUrl();
                                if (photoURL.startsWith("https://graph.facebook.com")) {
                                    photoURL = photoURL.concat("?height=100");
                                }
                                Glide.with(activity)
                                        .load(photoURL)
                                        .into(profile_picture);
                                temp_image.setVisibility(CardView.GONE);
                                profile_picture.setVisibility(CircleImageView.VISIBLE);
                            }
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
