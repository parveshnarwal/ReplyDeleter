package com.roadsidebaba.the.replydeleter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends Activity {

    private Button twitterLoginButton;
    private TextView headerText;

    public static final String PREF_NAME = "twitter_pref";
    public static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    public static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    public static final String PREF_KEY_TWITTER_LOGIN = "is_twitter_loggedin";
    public static final String PREF_USER_NAME = "twitter_user_name";
    public static final String PREF_USER_ID = "twitter_user_id";

    public static final int WEBVIEW_REQUEST_CODE = 100;


    private static Twitter twitter;
    private static RequestToken requestToken;

    private static SharedPreferences sharedPreferences;

    private String consumerKey = null;
    private String consumerSecret = null;
    private String callbackURL = null;
    private String oAuthVerifier = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        initializeTwitterAPI();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        twitterLoginButton = findViewById(R.id.login_button);
        headerText = findViewById(R.id.header_text);
        headerText.setTypeface(FontCache.get("font/segoesc.ttf", this));

        if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) {
            Toast.makeText(this, "Twitter consumer key or secret is not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPreferences = getSharedPreferences(PREF_NAME, 0);

        boolean isLoggedIn = sharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

        if (isLoggedIn) {

            String msg = "ALREADY LOGGED IN AS : ";
            String username = sharedPreferences.getString(PREF_USER_NAME, "");

            Toast.makeText(this, msg + username, Toast.LENGTH_LONG).show();

            GoToNonFollowers();

        } else {
            Uri uri = getIntent().getData();

            if (uri != null && uri.toString().startsWith(callbackURL)) {
                String verifier = uri.getQueryParameter(oAuthVerifier);

                try {

                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                    saveTwitterInfo(accessToken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        twitterLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(MainActivity.this, "Loading.. Please wait!", Toast.LENGTH_SHORT).show();

                loginToTwitter();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String verfier = data.getExtras().getString(oAuthVerifier);

            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verfier);

                Long userID = accessToken.getUserId();
                final User user = twitter.showUser(userID);
                String username = user.getName();

                saveTwitterInfo(accessToken);

                Toast.makeText(this, "Hi.. " + username, Toast.LENGTH_SHORT).show();

                GoToNonFollowers();

            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void initializeTwitterAPI() {
        consumerKey = getString(R.string.twitter_consumer_key);
        consumerSecret = getString(R.string.twitter_consumer_secret);
        callbackURL = getString(R.string.twitter_callback);

        oAuthVerifier = getString(R.string.twitter_oauth_verifier);
    }


    private void loginToTwitter() {
        boolean isLoggedIn = sharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

        if (!isLoggedIn) {
            final ConfigurationBuilder builder = new ConfigurationBuilder();

            builder.setOAuthConsumerKey(consumerKey);
            builder.setOAuthConsumerSecret(consumerSecret);

            final Configuration configuration = builder.build();

            final TwitterFactory factory = new TwitterFactory(configuration);

            twitter = factory.getInstance();

            try {
                requestToken = twitter.getOAuthRequestToken(callbackURL);

                final Intent intent = new Intent(this, WebView_Handler.class);

                intent.putExtra(WebView_Handler.EXTRA_URL, requestToken.getAuthenticationURL());

                startActivityForResult(intent, WEBVIEW_REQUEST_CODE);

            } catch (TwitterException e) {
                e.printStackTrace();
                ;
            }
        }

    }


    private void saveTwitterInfo(AccessToken accessToken) {
        Long userID = accessToken.getUserId();

        User user;

        try {
            user = twitter.showUser(userID);

            String username = user.getScreenName();

            SharedPreferences.Editor e = sharedPreferences.edit();
            e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
            e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
            e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
            e.putString(PREF_USER_NAME, username);
            e.putLong(PREF_USER_ID, userID);

            e.apply();


        } catch (TwitterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void GoToNonFollowers() {
        //Intent intent = new Intent(this, WelcomePage.class);
        //startActivity(intent);
    }


}
