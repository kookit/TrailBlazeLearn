package org.nus.trailblaze;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

import org.nus.trailblaze.dao.FacebookDao;
import org.nus.trailblaze.listeners.FacebookRegistryListener;
import org.nus.trailblaze.listeners.SignInFailureListener;
import org.nus.trailblaze.listeners.SignInListener;

import org.nus.trailblaze.dao.GoogleDao;
import org.nus.trailblaze.views.LearningTrailMainActivity;
import org.nus.trailblaze.views.RoleToggler;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrailBlazeMainActivity extends AppCompatActivity {

    public static final int GOOGLE_SIGN = 9001;
    public static final int FACEBOOK_SIGN = 64206;

    public FirebaseUser loggedInUser;
    private FirebaseAuth firebaseAuth;

    // required for google Login
    private GoogleSignInClient gClient;
    private GoogleDao gDao;


    // required for facebook login
    private LoginManager fmanager;
    private CallbackManager mCallback;
    private FacebookDao fDao;

    //initialize the controls
    @BindView(R.id.progressBar)
    ProgressBar bar;
    private EditText loginEmailText;
    private EditText loginPassText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // invoking the constructor and initializing the layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trail_blaze_main);
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        ButterKnife.bind(this);

        // CallbackManager to initalize the Facebook button
        fmanager = LoginManager.getInstance();
        mCallback = CallbackManager.Factory.create();
        gDao = new GoogleDao();
        fDao = new FacebookDao();
        gClient = gDao.getClient(this);

        Context currentContext = getApplicationContext();
        FacebookSdk.sdkInitialize(currentContext);
        // if the user has already logged in send them strait to next activity.
//        loggedInUser = fDao.getCurrent();
//        if(loggedInUser != null){
//            Intent next = new Intent(this, RoleToggler.class);
//            next.putExtra("user", this.fDao.fromFirebaseUser(loggedInUser));
//            startActivity(next);
//        }

        // initalize a callback to facebook button
        fmanager.registerCallback(mCallback, new FacebookRegistryListener(this, this.fDao, RoleToggler.class));
    }

    //Author: Priyanka Jalan (Email with Login and Password Feature)

    //Check if User is already logged in
    @Override
    protected void onStart(){
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null){
            sendToChooseLoginRole();
        }
    }

    //Login with Email and Password
    public void emailLoginClickListener(View view){
        loginEmailText = (EditText) findViewById(R.id.login_email_text);
        loginPassText = (EditText) findViewById(R.id.login_pass_text);
        String loginEmail = loginEmailText.getText().toString();
        String loginPass = loginPassText.getText().toString();
        // Call Firebase to Login With Email and Password
        firebaseAuth.signInWithEmailAndPassword(loginEmail,loginPass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    sendToChooseLoginRole();
                }
                else{
                    String errorMessage = task.getException().getMessage();
                    Toast.makeText(TrailBlazeMainActivity.this,"Error : " + errorMessage,Toast.LENGTH_LONG).show();
                }
            }
        });//END: Firebase Auth
    }

    private void sendToChooseLoginRole(){
        Intent chooseLoginRoleIntent = new Intent(TrailBlazeMainActivity.this, RoleToggler.class);
        startActivity(chooseLoginRoleIntent);
        finish();
    }


    // Google click listener
    public void googleClickListener(View view){
        Intent intent = gClient.getSignInIntent();
        startActivityForResult(intent, GOOGLE_SIGN);
    }

    // facebook click listener
    public void facebookClickListener(View view){
        fmanager.logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
    }

    // update on google account selected.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){

        Log.d("act", "got the code");
        Log.d("mtg", String.valueOf(requestCode));
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GOOGLE_SIGN){
            Log.d(String.valueOf(requestCode), data.toString());
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("account", account.toString());
                bar.setVisibility(bar.VISIBLE);
                this.gDao
                        .createFirebaseGoogleAuth(account)
                        .addOnCompleteListener(this,
                                new SignInListener(this, this.gDao, RoleToggler.class))
                        .addOnFailureListener(this, new SignInFailureListener(this));
            }
            catch (ApiException e){
                // handle exeception here.
                Log.d("Exception", e.toString());
                e.printStackTrace();
            }
        } else if(requestCode == FACEBOOK_SIGN) {
            bar.setVisibility(bar.VISIBLE);
            mCallback.onActivityResult(requestCode, resultCode, data);
        }
    }

}
