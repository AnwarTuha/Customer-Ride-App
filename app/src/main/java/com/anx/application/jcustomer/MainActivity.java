package com.anx.application.jcustomer;

import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private Button mLogin, googleSignIn;
    private ImageButton mRegister;
    private EditText mPhone, mPassword;
    private TextView tvLogin;
    private CountryCodePicker ccp;
    private String fullNumber;

    private static final int RC_SIGN_IN = 9001;

    private GoogleSignInClient mGoogleSignInClient;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){
                    Intent intent = new Intent(MainActivity.this, CustomerMapActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        googleSignIn = findViewById(R.id.googleSignIn);
        mLogin = findViewById(R.id.login);
        mRegister = findViewById(R.id.register);
        mPhone = findViewById(R.id.phone);
        mPassword = findViewById(R.id.password);
        tvLogin = findViewById(R.id.tvLogin);
        ccp = findViewById(R.id.ccp);

        googleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
                signIn();
            }
        });

        // Register User
        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                final String email = mEmail.getText().toString();
//                final String password = mPassword.getText().toString();
//                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener() {
//                    @Override
//                    public void onComplete(@NonNull Task task) {
//                        if (!task.isSuccessful()){
//                            Toast.makeText(MainActivity.this, "Sign Up Error", Toast.LENGTH_SHORT).show();
//                        } else {
//                            String userId = mAuth.getCurrentUser().getUid();
//                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
//                            current_user_db.setValue(true);
//                        }
//                    }
//                });
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);

                Pair[] pairs = new Pair[1];
                pairs[0] = new Pair<View, String>(tvLogin, "tvLogin");
                ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this, pairs);
                startActivity(intent, activityOptions.toBundle());
            }
        });


        // Login with user account
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                final String email = mEmail.getText().toString();
//                final String password = mPassword.getText().toString();
//                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (!task.isSuccessful()){
//                            Toast.makeText(MainActivity.this, "Login Error", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                });
            if (mPhone.getText().toString().trim().isEmpty() || mPhone.getText().toString().trim().length() < 9){
                mPhone.setError("Valid number is required");
                mPhone.requestFocus();
                return;
            }

            fullNumber = "+" + ccp.getFullNumber() + mPhone.getText().toString().trim();
            Intent intent = new Intent(MainActivity.this, VerifyPhoneActivity.class);
            intent.putExtra("phoneNumber", fullNumber);
            startActivity(intent);
            return;
            }
        });

    }

    private void  signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }


    public void signOut(){
        mAuth.signOut();
        mGoogleSignInClient.signOut();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("", "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed, "+e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        final String fullName = account.getDisplayName();
        final String email = account.getEmail();
        final String profileImage = account.getPhotoUrl().toString();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("anwar", "signInWithCredential:success");
                            Toast.makeText(MainActivity.this, "Sign in with credential Successful", Toast.LENGTH_SHORT).show();
                            boolean isNew = task.getResult().getAdditionalUserInfo().isNewUser();
                            Intent intent;
                            if (isNew) {
                                Toast.makeText(MainActivity.this, "New User", Toast.LENGTH_SHORT).show();
                                intent = new Intent(Intent.ACTION_VIEW);
                                intent.setClass(MainActivity.this, RegisterActivity.class);
                                intent.putExtra("fullName", fullName);
                                intent.putExtra("email", email);
                                intent.putExtra("profileImage", profileImage);
                                Log.e("profileImage", profileImage);

                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "Nebar user", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                if (user != null){
                                    intent = new Intent(MainActivity.this, CustomerMapActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("anwar", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }


    @Override
    public void onBackPressed() {
        finishAffinity();
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
