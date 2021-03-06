package develop.com.glitterbomb;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    Button mSettingButton;
    boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        MediaButtonIntentReceiver r = new MediaButtonIntentReceiver();
        filter.setPriority(1000);
        registerReceiver(r, filter);

        mSettingButton = findViewById(R.id.settingBtn);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();
    }

    public void mOnStartClick(View v) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.screenBrightness = 0;
        getWindow().setAttributes(params);

        startActivity(new Intent(MainActivity.this, RecordActivity.class));
    }

    public void mOnSettingClick(View v) {
        if (mSettingButton.getText().equals("Logout")) {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut();
            mSettingButton.setText("Sign in with Google");
        } else {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, 1);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mSettingButton.setText("Logout");
        } else {
            mSettingButton.setText("Sign in with Google");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 1) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately

            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (!isStarted) {
                    isStarted = true;
                    Toast.makeText(this, "Started recording!", Toast.LENGTH_SHORT).show();
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
                    mp.start();
                    startActivity(new Intent(MainActivity.this, RecordActivity.class));
                } else {
                    isStarted = false;
                    Toast.makeText(this, "Paused Recording!", Toast.LENGTH_LONG).show();
                    Uri pauseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    MediaPlayer mp2 = MediaPlayer.create(getApplicationContext(), pauseUri);
                    mp2.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                        @Override
                        public void onSeekComplete(MediaPlayer mediaPlayer) {
                            // TODO Auto-generated method stub
                            mediaPlayer.stop();
                        }
                    });
                    mp2.start();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (isStarted) {
                    isStarted = false;
                    Toast.makeText(this, "Paused Recording", Toast.LENGTH_LONG).show();
                    Uri pauseUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    MediaPlayer mp2 = MediaPlayer.create(getApplicationContext(), pauseUri);
                    mp2.start();
                }
                return true;
        }
        return false;
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("Sign-In", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Sign-In", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getApplicationContext(), "Success to sign in!", Toast.LENGTH_LONG).show();
                        mSettingButton.setText("Logout");
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("Sign-In", "signInWithCredential:failure", task.getException());
                    }
                }
            });
    }
}
