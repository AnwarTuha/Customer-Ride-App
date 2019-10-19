package com.anx.application.jcustomer;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Intent intent = new Intent(SplashScreen.this, CustomerMapActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
        return;

    }
}
