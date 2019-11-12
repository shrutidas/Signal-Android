package org.thoughtcrime.securesms.education;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;


import org.thoughtcrime.securesms.BaseActionBarActivity;
import org.thoughtcrime.securesms.ConversationListActivity;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.longmessage.LongMessageActivity;
import org.thoughtcrime.securesms.notifications.NotificationChannels;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.registration.PushChallengeRequest;
import org.thoughtcrime.securesms.util.CommunicationActions;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.util.Calendar;
import java.util.GregorianCalendar;


public class EducationActivity extends BaseActionBarActivity {

    Calendar launchTime = null;
    EducationalMessage em = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.education_activity);
        findViewById(R.id.education_more_button).setOnClickListener(v -> onTermsClicked());
        findViewById(R.id.welcome_continue_button).setOnClickListener(v -> onContinueClicked());
        TextView shortMessageView = findViewById(R.id.educationMessageView);
        em = EducationalMessageManager.getShortMessage(this);
        shortMessageView.setText( em.getMessageString(this));

        launchTime = GregorianCalendar.getInstance();

        EducationalMessageManager.notifyStatServer(this, EducationalMessageManager.MESSAGE_SHOWN, EducationalMessageManager.getMessageShownLogEntry(
                TextSecurePreferences.getLocalNumber(this), "splash-screen",  EducationalMessageManager.OPENING_SCREEN_MESSAGE, em.getMessageName(), launchTime.getTime(), -1));


    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    private void onTermsClicked() {
        //CommunicationActions.openBrowserLink(this, "https://signal.org/legal");

        Calendar exitTime = GregorianCalendar.getInstance();
        EducationalMessageManager.notifyStatServer(this, EducationalMessageManager.MESSAGE_SHOWN, EducationalMessageManager.getMessageShownLogEntry(
                TextSecurePreferences.getLocalNumber(this), "splash-screen-terms-exit",  EducationalMessageManager.OPENING_SCREEN_MESSAGE, em.getMessageName(), launchTime.getTime(), exitTime.getTimeInMillis()- launchTime.getTimeInMillis()));


        Intent intent = new Intent(this, LongEducationalMessageActivity.class);
        startActivity(intent);

    }

    private void onContinueClicked() {
        Permissions.with(this)
                .request(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE)
                .ifNecessary()
                .withRationaleDialog(getString(R.string.RegistrationActivity_signal_needs_access_to_your_contacts_and_media_in_order_to_connect_with_friends),
                        R.drawable.ic_contacts_white_48dp, R.drawable.ic_folder_white_48dp)
                .onAnyResult(() -> {
                    EducationalMessageManager.unarmEducation(EducationActivity.this);

                    Intent nextIntent = getIntent().getParcelableExtra("next_intent");

                    if (nextIntent == null) {
                        throw new IllegalStateException("Was not supplied a next_intent.");
                    }

                    Calendar exitTime = GregorianCalendar.getInstance();
                    EducationalMessageManager.notifyStatServer(this, EducationalMessageManager.MESSAGE_SHOWN, EducationalMessageManager.getMessageShownLogEntry(
                            TextSecurePreferences.getLocalNumber(this), "splash-screen-cont-exit",  EducationalMessageManager.OPENING_SCREEN_MESSAGE, em.getMessageName(), launchTime.getTime(), exitTime.getTimeInMillis()- launchTime.getTimeInMillis()));


                    startActivity(nextIntent);
                    overridePendingTransition(R.anim.slide_from_end, R.anim.fade_scale_out);
                    finish();
                })
                .execute();
    }
}

