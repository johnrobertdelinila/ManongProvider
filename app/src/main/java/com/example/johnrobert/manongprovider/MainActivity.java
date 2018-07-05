package com.example.johnrobert.manongprovider;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Patterns;
import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private Intent intent;
    private ProgressBar loginProgress;
    private MaterialButton loginButton;

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private AuthUI authUI = AuthUI.getInstance();
    private FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference rootRef = mDatabase.getReference();
    private DatabaseReference providerRef = rootRef.child("Providers");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent = new Intent(this, HomeActivity.class);
        loginProgress = findViewById(R.id.login_progress);

        final TextInputLayout passwordTextInput = findViewById(R.id.password_text_input);
        TextInputLayout emailTextInput = findViewById(R.id.email_text_input);

        final TextInputEditText passwordEditText = findViewById(R.id.password_edit_text);
        TextInputEditText emailEditText = findViewById(R.id.email_edit_text);

        loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(view -> {

            startActivity(new Intent(MainActivity.this, HomeActivity.class));

//            if (emailEditText.getText() == null || emailEditText.getText().length() == 0) {
//                emailTextInput.setError(getString(R.string.manong_error_email));
//                return;
//            }else {
//                emailEditText.setError(null);
//            }
//
//            if (!isEmailValid(emailEditText.getText())) {
//                emailTextInput.setError(getString(R.string.manong_error_email2));
//                return;
//            }else {
//                emailTextInput.setError(null);
//            }
//
//            if (!isPasswordInvalid(passwordEditText.getText())) {
//                passwordTextInput.setError(getString(R.string.manong_error_password));
//                return;
//            } else {
//                passwordTextInput.setError(null);
//            }
//
//            showProgressbar(true, loginButton);
//
//            mAuth.signInWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString())
//                    .addOnSuccessListener(authResult -> {
//                        String uid = authResult.getUser().getUid();
//                        // Checking if the user is a provider.
//                        verifyUserProvider(uid);
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(this, Objects.requireNonNull(e.getMessage()), Toast.LENGTH_SHORT).show();
//                        showProgressbar(false, loginButton);
//                    });

        });

        passwordEditText.setOnKeyListener((view, i, keyEvent) -> {
            if (isPasswordInvalid(passwordEditText.getText())) {
                passwordTextInput.setError(null);
            }
            return false;
        });

        emailEditText.setOnKeyListener((view, i, keyEvent) -> {
            if (isEmailValid(emailEditText.getText())) {
                emailTextInput.setError(null);
            }
            return false;
        });

    }

    private boolean isPasswordInvalid(Editable text) {
        return text != null && text.length() >= 6;
    }

    private boolean isEmailValid(Editable text) {
        return text != null && text.length() != 0 && Patterns.EMAIL_ADDRESS.matcher(text).matches();
    }

    private void showProgressbar(boolean isShow, MaterialButton materialButton) {
        if (isShow) {
            materialButton.setVisibility(MaterialButton.INVISIBLE);
            loginProgress.setVisibility(ProgressBar.VISIBLE);
        }else {
            materialButton.setVisibility(MaterialButton.VISIBLE);
            loginProgress.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    private void verifyUserProvider(String uid) {
        providerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(uid)) {
                    // User is verified as a provider.
                    // Go to next activity.
                    startActivity(intent);
                    showProgressbar(false, loginButton);
                }else {
                    // Sign out because the user is not a provider.
                    authUI.signOut(MainActivity.this)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Username or password is invalid.", Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(MainActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                showProgressbar(false, loginButton);
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showProgressbar(false, loginButton);
                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.manong_toolbar_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }
}
