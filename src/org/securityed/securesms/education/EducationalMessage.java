package org.securityed.securesms.education;

import android.content.Context;

public class EducationalMessage {

    private int stringID;
    private String messageName;


    public EducationalMessage(int stringID, String messageName){
        this.stringID = stringID;
        this.messageName = messageName;

    }

    public int getStringID() {
        return stringID;
    }

    public String getMessageName() {
        return messageName;
    }

    public String getMessageString(Context context) {
        return context.getResources().getString(getStringID());
    }
}
