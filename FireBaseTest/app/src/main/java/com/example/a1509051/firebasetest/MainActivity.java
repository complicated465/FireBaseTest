package com.example.a1509051.firebasetest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private TextView emailText,passwordText;
    private FirebaseUser mUser;

    private Button loginBtn;

    /* *************************************
     *              FACEBOOK               *
     ***************************************/
    /* The login button for Facebook */
    private LoginButton mFacebookLoginBtn;
    /* The callback manager for Facebook */
    private CallbackManager mFacebookCallbackManager;
    /* Used to track user logging in/out off Facebook */
    private AccessTokenTracker mFacebookAccessTokenTracker;
    private AccessToken mToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user= firebaseAuth.getCurrentUser();
                if (user != null) {
                    mUser=user;
                    // User is signed in
                    Log.d("test", "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d("test", "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };


        processView();

    }
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        mUser=mAuth.getCurrentUser();
        if(mUser!=null){
            String name = mUser.getDisplayName();
            String email = mUser.getEmail();
            Uri photoUrl = mUser.getPhotoUrl();

            boolean emailVerified = mUser.isEmailVerified();
            UserProfileChangeRequest q;
            String uid = mUser.getUid();
        }
        updateUI();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void processView() {
        emailText = (TextView)findViewById(R.id.emailText);
        passwordText = (TextView)findViewById(R.id.passwordText);
        loginBtn = (Button)findViewById(R.id.loginBtn);

        mFacebookCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginBtn = (LoginButton) findViewById(R.id.login_button);
        mFacebookLoginBtn.setReadPermissions(Arrays.asList(
                "public_profile", "email", "user_birthday"));
        mFacebookLoginBtn.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.i(TAG, "facebook login success");
                mToken = loginResult.getAccessToken();
                handleFacebookAccessToken(loginResult.getAccessToken());
                Log.d("FB","access token got.");

                //send request and call graph api

                GraphRequest request = GraphRequest.newMeRequest(
                        mToken,
                        new GraphRequest.GraphJSONObjectCallback() {

                            //當RESPONSE回來的時候

                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {

                                //讀出姓名 ID FB個人頁面連結
                                try {
                                    String email = object.getString("email");
                                    String birthday = object.getString("birthday");
                                }catch (JSONException ex){
                                    Log.e("FB","error:"+ex.getMessage());
                                }
                               // updateUI();
                            }
                        });

                //包入你想要得到的資料 送出request

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "facebook login cancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.i(TAG, "facebook login success" + exception.getMessage());
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            mUser = mAuth.getCurrentUser();
                            updateUI();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                           // updateUI(null);
                        }

                        // ...
                    }
                });
    }
    public void loginBtnOnClick(View v) {
       if(mUser!=null)
       {
           mAuth.signOut();
           updateUI();
       }else {
           String email = emailText.getText().toString();
           String password = passwordText.getText().toString();
           if (!validateForm(email, password)) {
               Toast.makeText(MainActivity.this, R.string.auth_failed,
                       Toast.LENGTH_SHORT).show();
               return;
           }
           mAuth.signInWithEmailAndPassword(email, password)
                   .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if (task.isSuccessful()) {
                               Log.i(TAG, "LoginUserWithEmail:onComplete:" + task.isSuccessful());

                               Toast.makeText(MainActivity.this, "Create Account Success",
                                       Toast.LENGTH_SHORT).show();
                               updateUI();
                           } else {
                               Log.i(TAG, "LoginUserWithEmail:failure:" + task.getException());
                               Toast.makeText(MainActivity.this, task.getException().getMessage(),
                                       Toast.LENGTH_LONG).show();
                           }
                           // ...
                       }
                   });
           /*mAuth.createUserWithEmailAndPassword(email, password)
                   .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if (task.isSuccessful()) {
                               Log.i(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                               Toast.makeText(MainActivity.this, "Create Account Success",
                                       Toast.LENGTH_SHORT).show();
                               updateUI();
                           } else {
                               Log.i(TAG, "createUserWithEmail:failure:" + task.getException());
                               Toast.makeText(MainActivity.this, task.getException().getMessage(),
                                       Toast.LENGTH_LONG).show();
                           }
                           // ...
                       }
                   });*/
       }
    }

    private boolean validateForm(String email,String password)
    {
        boolean valid = true;

        if (TextUtils.isEmpty(email)){
            emailText.setError("Required.");
            valid=false;
        }else{
            emailText.setError(null);
        }

        if(TextUtils.isEmpty(password)) {
            passwordText.setError("Required.");
            valid = false;
        }else{
            passwordText.setError(null);
        }


        return  valid;
    }

    private void updateUI(){
        mUser = mAuth.getCurrentUser();
        if(mUser!=null){
            emailText.setText(mUser.getEmail());
            loginBtn.setText(R.string.logout);
        }else{
            passwordText.setText("");
            loginBtn.setText(R.string.login);
        }
    }


}
