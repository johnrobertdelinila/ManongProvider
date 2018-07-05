package com.example.johnrobert.manongprovider;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class JobFragment extends Fragment {

    private Activity activity;
    private Context context;
    private int animatedRow = -1;
    private RecyclerView recyclerView;

    private DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference requestRef = rootRef.child("Request");
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private FirebaseRecyclerAdapter firebaseAdapter;
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

    // Might be in another Activity
    private static final String service_name = "Plumbing Repair";
    private static final String sample_message = "Hello from Philippines and all over the world!";
    public static final String ANONYMOUS = "Anonymous";
    private DatabaseReference quotesRef = rootRef.child("Quotes");
    private DatabaseReference customerRef = rootRef.child("Customers");
    private DatabaseReference providerRef = rootRef.child("Providers");
    private DatabaseReference messagesRef = rootRef.child("Messages");
    private String mUsername, mPhotoUrl;

    public JobFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_job, container, false);
        activity = getActivity();
        context = getContext();

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        if (user != null) {

            // Getting the name from Firebase User
            mUsername = user.getDisplayName();
            Log.e("USERNAME", mUsername);
            // Checking if the username is null,
            // Maybe get the name from the provider data.
            if (mUsername != null && mUsername.trim().equalsIgnoreCase("") || mUsername == null) {
                for (UserInfo userInfo : user.getProviderData()) {
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
                getUserRecord(user.getUid())
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Exception e = task.getException();
                                if (e instanceof FirebaseFunctionsException) {
                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                    FirebaseFunctionsException.Code code = ffe.getCode();
                                    Object details = ffe.getDetails();
                                    Toast.makeText(activity, "Error: " + String.valueOf(details), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                            String displayName = task.getResult();
                            if (displayName != null && displayName.trim().equals("") || displayName == null) {
                                displayName = ANONYMOUS;
                            }
                            mUsername = displayName;
                        });
            }

            Query queryServiceName = requestRef.orderByChild("serviceName").equalTo(service_name);

            SnapshotParser<Request> requestParser = snapshot -> {
                Request request = snapshot.getValue(Request.class);
                if (request != null) {
                    request.setKey(snapshot.getKey());
                }
                return request;
            };

            FirebaseRecyclerOptions<Request> options = new FirebaseRecyclerOptions.Builder<Request>()
                    .setQuery(queryServiceName, requestParser)
                    .build();

            firebaseAdapter = new FirebaseRecyclerAdapter<Request, RequestViewHolder>(options) {
                @NonNull
                @Override
                public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                    View view1 = LayoutInflater.from(context).inflate(R.layout.list_row_card, viewGroup, false);
                    return new RequestViewHolder(view1);
                }

                @Override
                protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull Request request) {

                    if (position > animatedRow) {
                        animatedRow = position;
                        long animationDelay = 200L + holder.getAdapterPosition() * 25;

//                        holder.itemView.setOnClickListener(view -> {
//                            Intent intent = new Intent(context, ChildActivity.class);
//                            intent.putExtra("request", request);
//                            String elementName = getString(R.string.transition_name_navigational_transition);
//                            ActivityOptionsCompat activityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, elementName);
//                            startActivity(intent, activityOptionsCompat.toBundle());
//                        });

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

                    holder.itemView.setOnClickListener(view12 -> {
                        // Checking if the Service Provider has already send a Quote.
                        requestRef.child(request.getKey()).child("quotes").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.hasChild(user.getUid())) {
                                    sendQuote(request);
                                }else {
                                    Toast.makeText(activity, "You've already send your quote to this request.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });

//                    holder.itemView.setOnClickListener(view12 -> addMessage(request.getUserId())
//                            .addOnCompleteListener(task -> {
//                                if (!task.isSuccessful()) {
//                                    Exception e = task.getException();
//                                    if (e instanceof FirebaseFunctionsException) {
//                                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
//                                        FirebaseFunctionsException.Code code = ffe.getCode();
//                                        Object details = ffe.getDetails();
//                                        Toast.makeText(activity, "Error: " + String.valueOf(details), Toast.LENGTH_SHORT).show();
//                                        return;
//                                    }
//                                }
//
//                                String displayName = task.getResult();
//                                if (displayName == null || displayName != null && displayName.trim().equals("")) {
//                                    displayName = "No name found.";
//                                }
//                                Toast.makeText(activity, displayName, Toast.LENGTH_SHORT).show();
//
//                            }));

                }
            };

            recyclerView.setAdapter(firebaseAdapter);
            firebaseAdapter.startListening();

        }

        return view;
    }

    private void sendQuote(Request request) {
        Quotes quotes = new Quotes();
        quotes.setTimestamp(ServerValue.TIMESTAMP);
        quotes.setServiceName(service_name);
        quotes.setRequestUserId(request.getUserId());
        quotes.setRequestId(request.getKey());
        quotes.setDate("Sample Date");
        HashMap<String, Integer> quote_price = new HashMap<>();
        quote_price.put("minimum", 1000);
        quote_price.put("maximum", 2000);
        quotes.setQuotePrice(quote_price);

        quotesRef.push().setValue(quotes, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                String key = databaseReference.getKey();
                HashMap<String, Object> quote = new HashMap<>();
                quote.put(user.getUid(), key);
                requestRef.child(request.getKey()).child("quotes").updateChildren(quote)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                createMessageCustomer(request.getUserId(), user.getUid());
                            }else {
                                Toast.makeText(activity, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }else {
                String errorMessage = databaseError.getMessage();
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createMessageCustomer(String requesterId, String serviceProviderId) {
        String messageLinkKey = requesterId + serviceProviderId;
        HashMap<String, Object> serviceProviderMessage = new HashMap<>();
        serviceProviderMessage.put(serviceProviderId, messageLinkKey);
        customerRef.child(requesterId).child("service_providers").updateChildren(serviceProviderMessage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        createMessageProvider(requesterId, messageLinkKey);
                    }else {
                        Toast.makeText(activity, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createMessageProvider(String requesterId, String messageLinkKey) {
        HashMap<String, Object> customerMessage = new HashMap<>();
        customerMessage.put(requesterId, messageLinkKey);
        providerRef.child(user.getUid()).child("customers").updateChildren(customerMessage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendIntroductionMessage(messageLinkKey);
                    }else {
                        Toast.makeText(activity, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendIntroductionMessage(String messageKey) {
        // Send the introductory message about the job.
        ManongMessage manongMessage = new ManongMessage(sample_message, mUsername, mPhotoUrl, null, user.getUid(), ServerValue.TIMESTAMP);
        messagesRef.child(messageKey).push().setValue(manongMessage)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(activity, "Your quote has been added to the request.", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(activity, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Task<String> getUserRecord(String text) {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        // For context or Pushing?
//        data.put("push", true);
        // Call callable function from Firebase Functions
        return mFunctions.getHttpsCallable("getUserRecord").call(data)
                .continueWith(task -> {
                    Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                    return (String) result.get("displayName");
                });

    }

}
