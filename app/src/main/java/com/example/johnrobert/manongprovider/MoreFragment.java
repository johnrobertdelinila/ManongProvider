package com.example.johnrobert.manongprovider;


import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.johnrobert.manongprovider.R;
import com.firebase.ui.auth.AuthUI;
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

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MoreFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CODE = 1000;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user =  mAuth.getCurrentUser();

    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = mDatabase.getReference();
    private DatabaseReference customerRef = rootRef.child("Providers");

    private Activity activity;

    private Switch switch_quotation, switch_messages, switch_jobs;

    public MoreFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        switch_quotation = view.findViewById(R.id.switch_quotation);
        switch_messages = view.findViewById(R.id.switch_messages);
        switch_jobs = view.findViewById(R.id.switch_completed_jobs);

        switch_quotation.setOnCheckedChangeListener(this);
        switch_messages.setOnCheckedChangeListener(this);
        switch_jobs.setOnCheckedChangeListener(this);

        activity = getActivity();

        getUserProfile(view);
        view.findViewById(R.id.profile_container).setOnClickListener(profileview -> {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
//            activity.startActivityForResult(intent, REQUEST_CODE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Bundle bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle();
                this.startActivity(intent, bundle);
            }else {
                startActivity(intent);
            }
        });

        view.findViewById(R.id.btn_logout).setOnClickListener(view1 -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
            dialog.setTitle("Logout");
            dialog.setMessage("Are you sure, you want to logout?");
            dialog.setPositiveButton("YES", (dialogInterface, i) -> AuthUI.getInstance()
                    .signOut(activity)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(activity, MainActivity.class);
                            activity.finish();
                            startActivity(intent);
                        }
                    }));
            dialog.setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss());

            AlertDialog dialogg = dialog.create();
            dialogg.show();

        });

        view.findViewById(R.id.btn_profile).setOnClickListener(view1 -> {
            Intent intent = new Intent(activity, MyProfileActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_gallery).setOnClickListener(view1 -> {
            Intent intent = new Intent(activity, GalleryActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btn_message).setOnClickListener(view1 -> {
            Intent intent = new Intent(activity, IntroMesageActivity.class);
            startActivity(intent);
        });

        if (user != null) {
            customerRef.child(user.getUid()).child("settings").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot: dataSnapshot.getChildren()) {
                        String value = childSnapshot.getKey();
                        if (value != null) {
                            Boolean b = childSnapshot.getValue(boolean.class);
                            if (b != null)  {
                                if (value.equalsIgnoreCase("New Completed Jobs")) {
                                    switch_jobs.setChecked(b);
                                }else if (value.equalsIgnoreCase("New Messages")) {
                                    switch_messages.setChecked(b);
                                }else if (value.equalsIgnoreCase("New Quotations")) {
                                    switch_quotation.setChecked(b);
                                }
                            }

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(activity, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    private void updateSettingsProfile(String value, boolean b) {
        if (user != null) {
            customerRef.child(user.getUid()).child("settings").child(value).setValue(b);
        }
    }

    private void getUserProfile(View view) {
        if (user != null) {
            CircleImageView userProfileImage = view.findViewById(R.id.user_profile_picture);
            TextView userDisplayName = view.findViewById(R.id.user_display_name);
            TextView viewEditProfile = view.findViewById(R.id.user_view_edit);

            String displayName = user.getDisplayName();
            String photoURL = String.valueOf(user.getPhotoUrl());

            if (activity != null && getContext() != null) {
                if (displayName == null || displayName != null && displayName.equalsIgnoreCase("")) {
                    displayName = "Anonymous";
                }
                userDisplayName.setText(displayName);
                viewEditProfile.setText(getResources().getString(R.string.manong_view_profile));

                if (photoURL != null && !photoURL.trim().equals("") && !photoURL.equals("null")) {
                    if (photoURL.startsWith("https://graph.facebook.com")) {
                        photoURL = photoURL.concat("?height=100");
                    }
                    Glide.with(activity).load(photoURL).into(userProfileImage);
                }
                view.findViewById(R.id.temp_image_view).setVisibility(CardView.GONE);
                userProfileImage.setVisibility(CircleImageView.VISIBLE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                String requestDisplayName = data.getStringExtra("displayName");
                String requestPhotoURL = data.getStringExtra("photoURL");

                if (requestDisplayName != null) {

                }
                if (requestPhotoURL != null) {

                }
                Toast.makeText(activity, "HELLO WORLD", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(activity, "HELLO WORLD", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        String value = compoundButton.getText().toString();
        updateSettingsProfile(value, b);
    }
}
