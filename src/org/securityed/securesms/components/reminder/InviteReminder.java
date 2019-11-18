package org.securityed.securesms.components.reminder;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import org.securityed.securesms.R;
import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.recipients.Recipient;
import org.securityed.securesms.util.concurrent.SignalExecutors;

public class InviteReminder extends Reminder {

  @SuppressLint("StaticFieldLeak")
  public InviteReminder(final @NonNull Context context,
                        final @NonNull Recipient recipient)
  {
    super(context.getString(R.string.reminder_header_invite_title),
          context.getString(R.string.reminder_header_invite_text, recipient.toShortString()));

    setDismissListener(v -> SignalExecutors.BOUNDED.execute(() -> {
      DatabaseFactory.getRecipientDatabase(context).setSeenInviteReminder(recipient.getId(), true);
    }));
  }
}
