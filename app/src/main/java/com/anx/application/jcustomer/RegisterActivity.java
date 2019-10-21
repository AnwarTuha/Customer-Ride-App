    package com.anx.application.jcustomer;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AppCompatActivity;

    import android.app.Activity;
    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.net.Uri;
    import android.os.Bundle;
    import android.provider.MediaStore;
    import android.text.TextUtils;
    import android.util.Log;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.animation.Animation;
    import android.view.animation.AnimationSet;
    import android.view.animation.AnimationUtils;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.RelativeLayout;
    import android.widget.Toast;

    import com.bumptech.glide.Glide;
    import com.bumptech.glide.load.engine.DiskCacheStrategy;
    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.OnFailureListener;
    import com.google.android.gms.tasks.OnSuccessListener;
    import com.google.android.gms.tasks.Task;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;
    import com.google.firebase.storage.UploadTask;

    import java.io.ByteArrayOutputStream;
    import java.io.IOException;
    import java.util.HashMap;
    import java.util.Map;

    import androidx.appcompat.widget.Toolbar;

    public class RegisterActivity extends AppCompatActivity {

        private RelativeLayout rlayout;
        private Animation animation;
        private Button mRegister;
        private EditText mEmail, mUsername, mPhone;
        private ImageView mProfileImage;
        private String userId;

        private Uri resultUri;
        String image_path;

        private FirebaseAuth mAuth;
        private DatabaseReference mCustomerDatabase;
        private FirebaseAuth.AuthStateListener firebaseAuthListener;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_register);

            Toolbar toolbar = findViewById(R.id.bgHeader);
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            rlayout = findViewById(R.id.rlayout);
            animation = AnimationUtils.loadAnimation(this, R.anim.uptodowndiagonal);
            rlayout.setAnimation(animation);

            mAuth = FirebaseAuth.getInstance();

            mRegister = findViewById(R.id.register);
            mEmail = findViewById(R.id.email);
            mUsername = findViewById(R.id.username);
            mPhone = findViewById(R.id.phone);

            String phoneNumber = getIntent().getStringExtra("phoneNumber");

            mPhone.setText(phoneNumber);

            // Authenticate User
            mAuth = FirebaseAuth.getInstance();
            firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        Intent intent = new Intent(RegisterActivity.this, CustomerMapActivity.class);
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
            };

            mProfileImage = (ImageView) findViewById(R.id.profileImage);
            Intent intent = getIntent();
            image_path= intent.getStringExtra("profileImage");
            if (image_path != null){
                resultUri = Uri.parse(image_path);
                Glide.with(getApplicationContext()).load(resultUri)
                        .thumbnail(0.5f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(mProfileImage);
                Log.e("profileImage", resultUri+"");
            }

            mProfileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1);
                }
            });

            String fullNameFromEmail = getIntent().getStringExtra("fullName");
            String accountFromEmail = getIntent().getStringExtra("email");

            if (fullNameFromEmail != null) {
                mUsername.setText(fullNameFromEmail     );
            }
            if (accountFromEmail != null){
                mEmail.setText(accountFromEmail);
            }

            mRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(mEmail.getText())) {
                        mEmail.setError("This field is required!");
                    } else if (TextUtils.isEmpty(mUsername.getText())) {
                        mUsername.setError("This field is required!");
                    } else if (TextUtils.isEmpty(mPhone.getText())) {
                        mPhone.setError("This field is required!");
                    } else {
                        final String email = mEmail.getText().toString();
                        final String userName = mUsername.getText().toString();
                        final String phone = mPhone.getText().toString();

                        userId = mAuth.getCurrentUser().getUid();
                        DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
                        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
                        current_user_db.setValue(true);
                        saveUserInformation(userName, phone, email);
                    }
                }
            });
        }


                private void saveUserInformation(String userName, String phone, String email) {
                    Map userInfo = new HashMap();
                    userInfo.put("email", email);
                    userInfo.put("name", userName);
                    userInfo.put("phone", phone);
                    mCustomerDatabase.updateChildren(userInfo);


                    if (resultUri != null) {
                        final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
                        Bitmap bitmap = null;

                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);
                        byte[] data = boas.toByteArray();
                        UploadTask uploadTask = filePath.putBytes(data);

                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                        });
                        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Task<Uri> downloadUri = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                                Map newImage = new HashMap();
                                newImage.put("profileImageUrl", downloadUri.toString());
                                mCustomerDatabase.updateChildren(newImage);
                                Toast.makeText(getApplicationContext(), "Image Uploaded Successfully", Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }
                        });
                    } else {
                        finish();
                    }
                }

                @Override
                public boolean onOptionsItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case android.R.id.home:
                            FirebaseAuth.getInstance().getCurrentUser().delete();
                            FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            return true;
                    }
                    return super.onOptionsItemSelected(item);
                }

                @Override
                protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
                    super.onActivityResult(requestCode, resultCode, data);
                    if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
                        final Uri imageUri = data.getData();
                        resultUri = imageUri;
                        mProfileImage.setImageURI(resultUri);
                    }
                }

                @Override
                protected void onStart() {
                    super.onStart();
                }

                @Override
                protected void onStop() {
                    super.onStop();
                    mAuth.removeAuthStateListener(firebaseAuthListener);
                }
        }



