package org.thoughtcrime.securesms.util;


import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.crypto.storage.TextSecureIdentityKeyStore;
import org.thoughtcrime.securesms.crypto.storage.TextSecureSessionStore;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.GroupDatabase;
import org.thoughtcrime.securesms.database.IdentityDatabase;
import org.thoughtcrime.securesms.database.IdentityDatabase.IdentityRecord;
import org.thoughtcrime.securesms.database.MessagingDatabase.InsertResult;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;
import org.thoughtcrime.securesms.sms.IncomingIdentityDefaultMessage;
import org.thoughtcrime.securesms.sms.IncomingIdentityUpdateMessage;
import org.thoughtcrime.securesms.sms.IncomingIdentityVerifiedMessage;
import org.thoughtcrime.securesms.sms.IncomingTextMessage;
import org.thoughtcrime.securesms.sms.OutgoingEducationalMessage;
import org.thoughtcrime.securesms.sms.OutgoingIdentityDefaultMessage;
import org.thoughtcrime.securesms.sms.OutgoingIdentityVerifiedMessage;
import org.thoughtcrime.securesms.sms.OutgoingTextMessage;
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture;
import org.thoughtcrime.securesms.util.concurrent.SettableFuture;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.multidevice.VerifiedMessage;

import java.util.List;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

public class EducationalUtil {

    private static final String TAG = IdentityUtil.class.getSimpleName();

    public static ListenableFuture<Optional<IdentityRecord>> getRemoteIdentityKey(final Context context, final Recipient recipient) {
        final SettableFuture<Optional<IdentityRecord>> future = new SettableFuture<>();

        new AsyncTask<Recipient, Void, Optional<IdentityRecord>>() {
            @Override
            protected Optional<IdentityRecord> doInBackground(Recipient... recipient) {
                return DatabaseFactory.getIdentityDatabase(context)
                        .getIdentity(recipient[0].getId());
            }

            @Override
            protected void onPostExecute(Optional<IdentityRecord> result) {
                future.set(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient);

        return future;
    }

    public static void sendEducationalMessage(Context context, Recipient recipient, boolean verified, boolean remote)
    {
        long                 time          = System.currentTimeMillis();
        SmsDatabase          smsDatabase   = DatabaseFactory.getSmsDatabase(context);
        GroupDatabase        groupDatabase = DatabaseFactory.getGroupDatabase(context);
        GroupDatabase.Reader reader        = groupDatabase.getGroups();

        GroupDatabase.GroupRecord groupRecord;

        boolean isChecked = false;


        OutgoingTextMessage outgoing;

        //if (true) outgoing = new OutgoingIdentityVerifiedMessage(getRecipient());
        //else          outgoing = new OutgoingIdentityDefaultMessage(getRecipient());

        outgoing = new OutgoingEducationalMessage(recipient);

        long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);

        Log.i(TAG, "Inserting verified outbox...");
        DatabaseFactory.getSmsDatabase(context).insertMessageOutbox(threadId, outgoing, false, time, null);




        /*

        // this loop deals with groups the the recipient is in... I think
        while ((groupRecord = reader.getNext()) != null) {

            if (groupRecord.getMembers().contains(recipient.getId()) && groupRecord.isActive() && !groupRecord.isMms()) {
                SignalServiceGroup group = new SignalServiceGroup(groupRecord.getId());

                if (remote) {
                    IncomingTextMessage incoming = new IncomingTextMessage(recipient.getId(), 1, time, null, Optional.of(group), 0, false);

                    if (verified) incoming = new IncomingIdentityVerifiedMessage(incoming);
                    else          incoming = new IncomingIdentityDefaultMessage(incoming);

                    smsDatabase.insertMessageInbox(incoming);
                } else {
                    RecipientId         recipientId    = DatabaseFactory.getRecipientDatabase(context).getOrInsertFromGroupId(GroupUtil.getEncodedId(group.getGroupId(), false));
                    Recipient           groupRecipient = Recipient.resolved(recipientId);
                    long                threadId       = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient);
                    OutgoingTextMessage outgoing ;

                    if (verified) outgoing = new OutgoingIdentityVerifiedMessage(recipient);
                    else          outgoing = new OutgoingIdentityDefaultMessage(recipient);

                    DatabaseFactory.getSmsDatabase(context).insertMessageOutbox(threadId, outgoing, false, time, null);
                }
            }
        }

        //this deals with the single person chat.
        if (remote) {
            IncomingTextMessage incoming = new IncomingTextMessage(recipient.getId(), 1, time, null, Optional.absent(), 0, false);

            if (verified) incoming = new IncomingIdentityVerifiedMessage(incoming);
            else          incoming = new IncomingIdentityDefaultMessage(incoming);

            Log.i(TAG, "Inserting verified outbox, is remote...");

            smsDatabase.insertMessageInbox(incoming);
        } else {
            OutgoingTextMessage outgoing;

            if (verified) outgoing = new OutgoingIdentityVerifiedMessage(recipient);
            else          outgoing = new OutgoingIdentityDefaultMessage(recipient);

            long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);

            Log.i(TAG, "Inserting verified outbox...");
            DatabaseFactory.getSmsDatabase(context).insertMessageOutbox(threadId, outgoing, false, time, null);
        }

         */
    }


}

