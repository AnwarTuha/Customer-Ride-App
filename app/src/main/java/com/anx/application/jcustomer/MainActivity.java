package com.anx.application.jcustomer;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button mLogin;
    private ImageButton mRegister;
    private EditText mPhone, mPassword;
    private TextView tvLogin;
    private CountryCodePicker ccp;
    private String fullNumber;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Authenticate User
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

        mLogin = findViewById(R.id.login);
        mRegister = findViewById(R.id.register);
        mPhone = findViewById(R.id.phone);
        mPassword = findViewById(R.id.password);
        tvLogin = findViewById(R.id.tvLogin);
        ccp = findViewById(R.id.ccp);


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
