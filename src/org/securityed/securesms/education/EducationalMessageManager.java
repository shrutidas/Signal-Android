package org.securityed.securesms.education;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.security.ProviderInstaller;

import org.securityed.securesms.ApplicationContext;
import org.securityed.securesms.R;
import org.securityed.securesms.push.SignalServiceNetworkAccess;
import org.securityed.securesms.util.TextSecurePreferences;
import org.w3c.dom.Text;

import java.net.ConnectException;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


/**
 *
 * This class implements the logic of when and where to show the educational messages.
 * Each UI controller for the X places we can possibly show messages call isTimeForShortMessage()
 * followed by getShortMessage() if the initial call returns true.
 *
 * @author Omer
 */

public class EducationalMessageManager {

    //message place codes.
    public static final int IN_CONVERSATION_MESSAGE = 0;
    public static final int TOOL_TIP_MESSAGE = 1;
    public static final int OPENING_SCREEN_MESSAGE = 2;
    public static final int LONG_MESSAGE_SCREEN = 3;

    public static final String MESSAGE_SHOWN = "message_shown";
    public static final String MESSAGE_EXCHANGE = "message_exchange";

    public static final int GET_SMS_CODE = 0;
    public static final int GET_PHONE_NUM = 1;
    public static final int CONTROL_PHONE_NUM = 2;

    private static String serverResponseCode = null;




    //private static final int[] shortMessages = {R.string.short_v1, R.string.short_v2, R.string.short_v3, R.string.short_v4, R.string.short_v5};
    private static final EducationalMessage[] shortMessages =
            {new EducationalMessage(R.string.short_v1, "short-v1"),
            new EducationalMessage(R.string.short_v2, "short-v2"),
            new EducationalMessage(R.string.short_v3, "short-v3"),
            new EducationalMessage(R.string.short_v4, "short-v4"),
            new EducationalMessage(R.string.short_v5, "short-v5")};

    private static final int[] mediumMessages = {R.string.mid_v1, R.string.mid_v2};
    private static final int longMessage = R.string.longMessage;


    public static boolean isTimeForShortMessage(Context context, int messagePlaceCode){
        if(!TextSecurePreferences.isExperimentalGroup(context)){
            return false;
        }

        switch(messagePlaceCode){
        case IN_CONVERSATION_MESSAGE:
            return false;
        case TOOL_TIP_MESSAGE:
            return true;
        case OPENING_SCREEN_MESSAGE:
            //TODO migrate this function to this class.
            return TextSecurePreferences.hasNotSeenEducationalMessageInAWhile(context);
        default:
            return false;
        }

    }

    //TODO fix this thing. we want the timezone together with the unix time.
    private static String formatDateString( Date date){
        return date.toString().replace(" ", "_").replace(":", "-") + "_" + date.getTime();
    }

    public static String getMessageShownLogEntry( String phoneNumber, String screen, int messageType, String version, Date date, long timeElapsed){
        return phoneNumber + "_" + screen + "_" + messageType + "_" + version + "_" + formatDateString(date) + "_" + timeElapsed;
    }

    public static String getMessageExchangeLogEntry(String phoneNumber, Boolean sent, String messageType, Date date){
        return phoneNumber + "_" + sent + "_" + messageType + "_" + formatDateString(date);
    }

    public static void notifyStatServer(Context context, String args ){

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://akgul.cs.umd.edu/post_stats/?" + args;

        StringRequest stringRequest = new StringRequest(StringRequest.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                Log.d("request sent to server", response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e("uhh", error.toString());

                TextSecurePreferences.addToUnsentLogs(context, args);
            }
        });

        queue.add(stringRequest);

    }

    public static void notifyStatServer(Context context, String logType, String log){
        String args = logType + "=" + log;
        notifyStatServer(context, args);
    }


    public static String registrationRequest(String phoneNumber, Context context, int request_type){

        String url = "";
        switch (request_type){
            case GET_SMS_CODE:
                url = "https://akgul.cs.umd.edu/register/?get_sms_code=" + phoneNumber;
                break;
            case GET_PHONE_NUM:
                url = "https://akgul.cs.umd.edu/register/get_phone_num/";
                break;
            case CONTROL_PHONE_NUM:
                url = "https://akgul.cs.umd.edu/register/can_use/?num=" + phoneNumber;
                break;
        }

        return serverRequestBlocking(context, url);


        //  https://akgul.cs.umd.edu/register/?get_recent_code=+12015847635
    }

    public static String serverRequestBlocking(Context context, String url){

        RequestQueue queue = Volley.newRequestQueue(context);

        String serverResponse = null;

        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest stringRequest = new StringRequest(StringRequest.Method.GET, url, future, future);
        queue.add(stringRequest);

        Log.d("blocking req", url);

        try {
            serverResponse = future.get(); // this will block
            Log.d("volley block response", serverResponse);
        } catch (InterruptedException e) {
            Log.d("volley blocking", e.getMessage());
        } catch (ExecutionException e) {
            Log.d("volley blocking", e.getMessage());
        } catch (Exception e){
            Log.d("volley blocking", e.toString());
        }

        return serverResponse;
    }


    //these are what we use to make sure the welcome educational message only appears on app launch
    //and not with every activity launch.
    //If it's time, the onClose method arms the message
    //With the next run the message is displayed and message disarmed so we don't show it again.
    //NOT a school shooting joke.
    public static boolean isEducationArmed(Context context){
        return TextSecurePreferences.isEducationArmed(context);
    }
    public static void armEducation(Context context){
        TextSecurePreferences.armEducation(context);
    }

    public static void unarmEducation(Context context){
        TextSecurePreferences.unarmEducation(context);
    }
    public static String getShortMessageString(Context context){
        return getShortMessage(context).getMessageString(context);
    }

    public static int getShortMessageID(Context context){
        return getShortMessage(context).getStringID();
    }

    public static EducationalMessage getShortMessage(Context context){
        int times_opened = TextSecurePreferences.incrementLastMessageSeen(context);
        return shortMessages[times_opened % shortMessages.length];
    }

    public static EducationalMessage getEducationalMessageFromBody( Context context, String body){

        Log.d("EM search body", body);
        for( EducationalMessage em:shortMessages){
            if( body.equals(em.getMessageString(context))){
                return em;
            }
        }

        return null;
    }

    public static String getMidMessage(Context context){
        return getString(context, mediumMessages[0]);
    }

    public static String getLongMessage(Context context){
        return getString(context, longMessage);
    }

    private static String getString(Context context, int stringID){
        return context.getResources().getString(stringID);
    }

    public static void sendUnsentLogs(Context context){

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {

                String[] logs = TextSecurePreferences.getUnsentLogs(context);
                TextSecurePreferences.deleteUnsentLogs(context);

                for(String log:logs){
                    notifyStatServer(context, log);
                    SystemClock.sleep(200);
                }

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
