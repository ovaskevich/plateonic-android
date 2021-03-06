package com.plateonic.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.plateonic.R;
import com.plateonic.android.com.plateonic.utils.FacebookDetails;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public class LoginActivity extends FragmentActivity {

    public static final int SPLASH_SCREEN_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_login);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    public static class LoginFragment extends Fragment implements Session.StatusCallback {

        private boolean mDontDelay = false;
        private FacebookDetails mLatestFbDetails;

        // ui
        private Button mAuth;
        private ImageView mLogo;

        // technically we need to have this on every fragment to keep track of the choose state
        // but for the purposes of this hackathon we'll just have it in that LoginFragment
        private UiLifecycleHelper mUiLifecycleHelper;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_login, container, false);
            mUiLifecycleHelper = new UiLifecycleHelper(getActivity(), this);
            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mAuth = (Button) getView().findViewById(R.id.authButton);
            mLogo = (ImageView) getView().findViewById(R.id.app_logo);

            // fade in the plate
            Animation spinIn = AnimationUtils.loadAnimation(getActivity(), R.anim.spin_in);
            spinIn.setStartOffset(200);
            mLogo.startAnimation(spinIn);

            // slide in the title
//            ImageView title = (ImageView) getView().findViewById(R.id.app_title);
//            Animation slideIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
//            slideIn.setDuration(1000);
//            slideIn.setInterpolator(getActivity(), android.R.interpolator.anticipate_overshoot);
//            slideIn.setStartOffset(1000);
//            title.startAnimation(slideIn);

            Session session = Session.getActiveSession();
            if (session == null) {
                session = Session.openActiveSessionFromCache(getActivity());
            }

            if (session != null && session.isOpened()) {
                getUserDetailsAndContinue();
            } else {
                // don't delay if we need to wait for the user to press the Login button
                mDontDelay = true;

                // also face in the fb button
                mAuth = (Button) getView().findViewById(R.id.authButton);
                Animation authAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_slide_in_bottom);
                authAnim.setStartOffset(650);
                authAnim.setDuration(700);
                authAnim.setFillAfter(true);
                mAuth.startAnimation(authAnim);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            mUiLifecycleHelper.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
            mUiLifecycleHelper.onPause();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mUiLifecycleHelper.onDestroy();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            mUiLifecycleHelper.onSaveInstanceState(outState);
        }

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened()) {
                getUserDetailsAndContinue();
            }
        }

        protected void getUserDetailsAndContinue() {


            // also get user's name
            Request.executeMeRequestAsync(Session.getActiveSession(), new Request.GraphUserCallback() {

                // callback after Graph API response with user object
                @Override
                public void onCompleted(GraphUser user, Response response) {
                    if (user != null) {
                        //Toast.makeText(getActivity(), "Hello, " + user.getFirstName() + "!", Toast.LENGTH_LONG).show();
                        mLatestFbDetails = new FacebookDetails(user.getId(), user.getFirstName(), user.getLastName(), user.getName());
                        mAuth.setEnabled(false);
                        mAuth.setText("Loading...");

                        // once the session is open, continue to the next activity
                        new Handler().postDelayed(new GoToNextActivityRunnable(LoginFragment.this), mDontDelay ? 800 : SPLASH_SCREEN_DELAY_MS);
                    }
                }
            });

        }

        private static class GoToNextActivityRunnable implements Runnable {

            // hold a weak reference so we don't leak
            private final WeakReference<LoginFragment> fragment;

            GoToNextActivityRunnable(LoginFragment fragment) {
                this.fragment = new WeakReference<LoginFragment>(fragment);
            }

            @Override
            public void run() {
                LoginFragment a = fragment.get();
                if (a != null && a.isResumed() && !a.isDetached()) {
                    Intent i = new Intent(a.getActivity(), ChooseWhereEatActivity.class);
                    i.putExtra("fb", a.getLatestFacebookInfo());
                    a.startActivity(i);
                    a.getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    a.getActivity().finish();
                }
            }
        }

        private Serializable getLatestFacebookInfo() {
            return mLatestFbDetails;
        }
    }

}
