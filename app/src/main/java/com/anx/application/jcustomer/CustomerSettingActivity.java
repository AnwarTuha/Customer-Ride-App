package com.anx.application.jcustomer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AuthProvider;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettingActivity extends AppCompatActivity {

    private Button mConfirm, mBack, mDelete;
    private EditText mNameField, mPhoneField, mEmailField;
    private ImageView mProfileImage;
    private ProgressBar mEmailProgressBar, mNameProgressBar, mPhoneProgressBar;

    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;

    private String userId;
    private String mName;
    private String mEmail;
    private String mPhone;
    private String mProfileImageUrl;

    private CountryCodePicker ccp;

    private Uri resultUri;

    FirebaseUser firebaseUser;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_setting);

        mProfileImage = (ImageView) findViewById(R.id.profileImage);

        mConfirm = findViewById(R.id.confirm);
        Toolbar toolbar = findViewById(R.id.bgHeader);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDelete = findViewById(R.id.delete);
        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mEmailField = findViewById(R.id.email);

        mEmailProgressBar = findViewById(R.id.emailProgress);
        mEmailProgressBar.setVisibility(View.VISIBLE);

        mNameProgressBar = findViewById(R.id.nameProgress);
        mNameProgressBar.setVisibility(View.VISIBLE);

        mPhoneProgressBar = findViewById(R.id.phoneProgress);
        mPhoneProgressBar.setVisibility(View.VISIBLE);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();

        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        getUserInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(CustomerSettingActivity.this)
                        .setTitle("Delete Account")
                        .setMessage("are you sure you want to delete this account?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteRegisteredUser();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create()
                        .show();
            }
        });

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });
    }

    private void deleteRegisteredUser() {
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        if (customerRef != null){
            customerRef.removeValue();
            FirebaseAuth.getInstance().getCurrentUser().delete();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(CustomerSettingActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    private void getUserInfo(){
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    mEmailProgressBar.setVisibility(View.VISIBLE);
                    mNameProgressBar.setVisibility(View.VISIBLE);
                    mPhoneProgressBar.setVisibility(View.VISIBLE);

                    if (map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                        mNameProgressBar.setVisibility(View.GONE);
                    }
                    if (map.get("email") != null){
                        mEmail = map.get("email").toString();
                        mEmailField.setText(mEmail);
                        mEmailProgressBar.setVisibility(View.GONE);
                    }
                    if (map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone.replace("+251", ""));
                        mPhoneProgressBar.setVisibility(View.GONE);
                    }
                    if (dataSnapshot.child("profileImageUrl").getValue() != null){
                        StorageReference storageReference =  FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mProfileImage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void saveUserInformation(){
        dialog = ProgressDialog.show(CustomerSettingActivity.this, "",
                "Saving. Please wait...", true);
        mName = mNameField.getText().toString();
        ccp = findViewById(R.id.ccp);

        mPhone = "+" + ccp.getFullNumber() + mPhoneField.getText().toString().trim();
        Map userInfo = new HashMap();
        userInfo.put("name", mName);
        userInfo.put("phone", mPhone);
        mCustomerDatabase.updateChildren(userInfo);

        if (resultUri != null){
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
                    dialog.cancel();
                    finish();
                    return;
                }
            });
        } else {
            if (dialog.isShowing()){
                dialog.cancel();
            }
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mProfileImage.setImageURI(resultUri);
        }
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
                onBackPressed();
                return true;
    }


}
