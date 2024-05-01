package com.app.abc.activity;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialException;

import com.app.abc.R;
import com.app.abc.databinding.ActivitySignInBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = SignInActivity.class.getSimpleName();
    private ActivitySignInBinding binding;
    private GetGoogleIdOption googleIdOption;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Clear views to prevent memory leaks
        binding = null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initialize UI Binding class
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.signInBtn.setOnClickListener(view -> signIn());

        googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();
    }

    private void signIn() {

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CancellationSignal cancellationSignal = new CancellationSignal();
        Executor executor = Executors.newCachedThreadPool();

        CredentialManager credentialManager = CredentialManager.create(SignInActivity.this);
        credentialManager.getCredentialAsync(SignInActivity.this, request, cancellationSignal, executor, new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse getCredentialResponse) {
                Log.d(TAG, "GetCredentialResponse: " + getCredentialResponse.getCredential());
                handleSignIn(getCredentialResponse);
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                Log.e(TAG, "GetCredentialException: " + e.getMessage());
            }
        });
    }

    public void handleSignIn(GetCredentialResponse result) {
        // Handle the successfully returned credential.
        Credential credential = result.getCredential();

        if (credential instanceof PublicKeyCredential) {
            String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
            // Share responseJson i.e. a GetCredentialResponse on your server to validate and authenticate
            Log.d(TAG, "Response: " + responseJson);
        } else if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            // Use id and password to send to your server to validate and authenticate
            Log.d(TAG, "Username: " + username + " :: " + " Password: " + password);
        } else if (credential instanceof CustomCredential) {
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                try {
                    // Use googleIdTokenCredential and extract id to validate and
                    // authenticate on your server
                    GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());

                    //Initialize AuthCredential with googleIdTokenCredential
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.getIdToken(), null);
                    //Sign In user with firebase auth
                    FirebaseAuth.getInstance().signInWithCredential(authCredential).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            //TODO Login Success Update the UI as you want
                            Log.d(TAG, "Login success");
                        } else {
                            Log.e(TAG, "Login failed");
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Exception: ", e);
                }
            } else {
                // Catch any unrecognized custom credential type here.
                Log.e(TAG, "Unexpected type of credential");
            }
        } else {
            // Catch any unrecognized credential type here.
            Log.e(TAG, "Unexpected type of credential");
        }
    }
}
