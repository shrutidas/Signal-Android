package org.securityed.securesms.sms;

/*
*
* Similar to other outgoing messages but used to display our educational messages.
* The body of the messages will probably be controlled in ConversionUpdateItem.java
* */

import org.securityed.securesms.education.EducationalMessage;
import org.securityed.securesms.recipients.Recipient;

import java.util.Date;
import java.util.GregorianCalendar;

public class OutgoingEducationalMessage extends OutgoingTextMessage {

    private EducationalMessage em;
    private Date timeCreated;

    public OutgoingEducationalMessage(Recipient recipient) {

        this(recipient, "");
    }

    private OutgoingEducationalMessage(Recipient recipient, String body) {
        super(recipient, body, -1);
        timeCreated = GregorianCalendar.getInstance().getTime();
    }

    @Override
    public boolean isEducationalMessage() {
        return true;
    }

    @Override
    public OutgoingTextMessage withBody(String body) {
        return new OutgoingEducationalMessage(getRecipient(), body);
    }
}