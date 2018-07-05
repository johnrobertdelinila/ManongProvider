package com.example.johnrobert.manongprovider;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Gallery;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class GalleryActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE = 2;
    private static final String TAG = "GalleryActivity";

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseUser user = mAuth.getCurrentUser();
    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = mDatabase.getReference();
    private DatabaseReference providerRef = rootRef.child("Providers");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        setUpToolbar();

        findViewById(R.id.share_button).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

    }

    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();

                    if (uri != null) {
                        // Ask user
                        StorageReference providerSharePhotos = FirebaseStorage.getInstance().getReference()
                                .child("Provider share photos").child(user.getUid()).child(uri.getLastPathSegment());
                        putImageInStorage(providerSharePhotos, uri);
                    }
                }
            }
        }

    }

    private void putImageInStorage(final StorageReference storageReference, Uri uri) {

        UploadTask uploadTask = storageReference.putFile(uri);

        uploadTask.addOnCompleteListener(GalleryActivity.this,
                task -> {
                    if (!task.isSuccessful() || !task.isComplete()) {
                        Log.w(TAG, "Image upload task was not successful.",
                                task.getException());
                    }
                });

        uploadTask
                .continueWithTask(task -> storageReference.getDownloadUrl())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String url = task.getResult().toString();
                        // Update profile
                        addSharePhoto(url);
                    }else {
                        if (task.getException() != null) {
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void addSharePhoto(String url) {
        DatabaseReference sharedPhotoRef = providerRef.child(user.getUid()).child("Photos").push().child(url);
        sharedPhotoRef.setValue(url).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Successfully added a photo
                Toast.makeText(this, "Photo added successfully.", Toast.LENGTH_SHORT).show();
            }else {
                if (task.getException() != null) {
                    Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
