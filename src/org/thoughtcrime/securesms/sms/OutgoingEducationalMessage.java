package org.thoughtcrime.securesms.sms;

/*
*
* Similar to other outgoing messages but used to display our educational messages.
* The body of the messages will probably be controlled in ConversionUpdateItem.java
* */

import org.thoughtcrime.securesms.recipients.Recipient;

public class OutgoingEducationalMessage extends OutgoingTextMessage {

    private int educationalMessageVersion;

    public OutgoingEducationalMessage(Recipient recipient) {
        this(recipient, "", 1);
    }

    private OutgoingEducationalMessage(Recipient recipient, String body, int educationalMessageVersion) {
        super(recipient, body, -1);
        this.educationalMessageVersion = educationalMessageVersion;
    }

    @Override
    public boolean isEducationalMessage() {
        return true;
    }

    @Override
    public OutgoingTextMessage withBody(String body) {
        return new OutgoingEducationalMessage(getRecipient(), body, educationalMessageVersion);
    }
}