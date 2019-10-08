package org.thoughtcrime.securesms.education;

import android.content.Context;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

public class EducationalMessageManager {



    private static final int[] shortMessages = {R.string.short_v1, R.string.short_v2, R.string.short_v3, R.string.short_v4, R.string.short_v5};
    private static final int[] mediumMessages = {R.string.mid_v1, R.string.mid_v2};
    private static final int longMessage = R.string.longMessage;


    public static boolean isTimeForShortMessage(Context context){


        return true;
    }



    public static String getShortMessage(Context context){
        return getString(context, getShortMessageID(context));
    }

    public static int getShortMessageID(Context context){
        int times_opened = TextSecurePreferences.incrementLastMessageSeen(context);
        return shortMessages[times_opened % shortMessages.length];
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
}
