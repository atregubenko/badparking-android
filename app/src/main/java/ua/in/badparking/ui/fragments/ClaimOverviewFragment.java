package ua.in.badparking.ui.fragments;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.inject.Inject;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ua.in.badparking.App;
import ua.in.badparking.R;
import ua.in.badparking.events.AuthorizedWithFacebookEvent;
import ua.in.badparking.events.ClaimPostedEvent;
import ua.in.badparking.events.ImageUploadedEvent;
import ua.in.badparking.model.Claim;
import ua.in.badparking.model.MediaFile;
import ua.in.badparking.model.User;
import ua.in.badparking.services.ClaimState;
import ua.in.badparking.services.UserState;
import ua.in.badparking.services.api.ClaimsService;
import ua.in.badparking.services.api.TokenService;
import ua.in.badparking.services.api.UserService;
import ua.in.badparking.ui.activities.MainActivity;
import ua.in.badparking.ui.adapters.PhotoAdapter;

/**
 * Design https://www.dropbox.com/sh/vbffs09uqzaj2mt/AAABkTvQbP7q10o5YP83Mzdia?dl=0
 * Created by Dima Kovalenko on 7/3/16.
 */
public class ClaimOverviewFragment extends BaseFragment {

    @BindView(R.id.recyclerView)
    protected RecyclerView recyclerView;

    @BindView(R.id.send_button)
    Button mSendButton;

    @BindView(R.id.crimeTypesTextView)
    TextView crimeTypeTextView;

    @BindView(R.id.addressTextView)
    TextView addressTextView;

    @BindView(R.id.login_button)
    LoginButton loginButton;
    private Unbinder unbinder;

    @Inject
    private ClaimsService mClaimService;

    @Inject
    private UserService userService;

    @Inject
    private TokenService mTokenService;

    private AlertDialog waitDialog;
    private AlertDialog readyDialog;

    private PhotoAdapter photoAdapter;
    private CallbackManager callbackManager;

    public static Fragment newInstance() {
        return new ClaimOverviewFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_claim_overview, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        EventBus.getDefault().register(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        unbinder.unbind();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        mVerificationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //TODO: verify user with bank id
        mSendButton.setEnabled(true);


////        }
//            }
//        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);

        photoAdapter = new PhotoAdapter(getActivity());
        recyclerView.setAdapter(photoAdapter);
        recyclerView.setHasFixedSize(true);

        callbackManager = CallbackManager.Factory.create();

        loginButton.setReadPermissions("email, public_profile");
        // If using in a fragment
        loginButton.setFragment(this);
        // Other app specific specialization
        loginButton.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        loginButton.setVisibility(View.GONE);
                        mSendButton.setVisibility(View.VISIBLE);
                        userService.authorizeWithFacebook(loginResult.getAccessToken().getToken());
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                    }
                });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()){
            crimeTypeTextView.setText(ClaimState.INST.getSelectedCrimeTypesNames());
            addressTextView.setText(ClaimState.INST.getFullAddress());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLoggedIn()) {
            loginButton.setVisibility(View.GONE);
            mSendButton.setVisibility(View.VISIBLE);
        } else {
            loginButton.setVisibility(View.VISIBLE);
            mSendButton.setVisibility(View.GONE);
        }
    }

    private void send() {
        if (!ClaimState.INST.getClaim().isComplete()) {
            // TODO show "not complete" message
            return;
        } else if (UserState.INST.getUser() == null) {
            loginButton.setVisibility(View.VISIBLE);
            mSendButton.setVisibility(View.GONE);
            return;
        }
        final Claim claim = ClaimState.INST.getClaim();
        final User user = UserState.INST.getUser();
        showSendClaimDialog();
        mClaimService.postMyClaims(claim);
    }


    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

//TODO: Verify token
//    @Subscribe
//    public void onTokenVerified(TokenVerifiedEvent event) {
////        if(event.getVerificationResult().equals(false)) {
//            String url = Constants.BASE_URL + "/profiles/login/dummy";
//            get(url, new Callback() {
//                @Override
//                public void onFailure(Request request, IOException e) {
////                    EventBus.getDefault().post(new ClaimPostedEvent(e.getMessage()));
//                }
//
//                @Override
//                public void onResponse(Response response) throws IOException {
//                    ClaimState.INST.setToken(response.headers().get("X-JWT"));
//                }
//            });
////        }
//        final Claim claim = ClaimState.INST.getClaim();
//        final User user = UserState.INST.getUser();
//        //TODO: 1. Add user data to request. 2. TBD - upload image
//        showSendClaimDialog();
//        mClaimService.postMyClaims(claim);
//    }

    private void showSendClaimDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(App.getAppContext().getString(R.string.claim_sending));
        waitDialog = builder.create();
        waitDialog.show();
    }

    @Subscribe
    public void onImagePosted(final ImageUploadedEvent event) {
        readyDialog.hide();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if(event.getImageCounter() != -1) {
            builder.setMessage(App.getAppContext().getString(R.string.photo_uploaded) + event.getImageCounter() + "/" + ClaimState.INST.getPictures().size());
        } else {
            builder.setMessage(App.getAppContext().getString(R.string.error_uploading_image));
        }
        if (ClaimState.INST.getPictures().size() == event.getImageCounter()) {
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    ((MainActivity) getActivity()).moveToFirst();
                }
            });
        }
        readyDialog = builder.create();
        readyDialog.show();
    }

    @Subscribe
    public void onClaimPosted(final ClaimPostedEvent event) {
        ClaimState.INST.setPk(event.getPk());
        waitDialog.hide();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(event.getMessage());
        readyDialog = builder.create();
        readyDialog.show();

        if(event.getPosted()) {
            List<MediaFile> files = ClaimState.INST.getPictures();
            for (int i = 0; i < files.size() + 1; i++) {
                MediaFile file = files.get(i);
                mClaimService.postImage(event.getPk(), file, i + 1);
            }
        }

    }


    @Subscribe
    public void onAuthorizedWithFacebook(final AuthorizedWithFacebookEvent event) {
        User user = new User();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}