package org.securityed.securesms.database.loaders;


import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;

import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.recipients.Recipient;
import org.securityed.securesms.recipients.RecipientId;
import org.securityed.securesms.util.AbstractCursorLoader;

public class ThreadMediaLoader extends AbstractCursorLoader {

  private final RecipientId recipientId;
  private final boolean     gallery;

  public ThreadMediaLoader(@NonNull Context context, @NonNull RecipientId recipientId, boolean gallery) {
    super(context);
    this.recipientId = recipientId;
    this.gallery     = gallery;
  }

  @Override
  public Cursor getCursor() {
    long threadId = DatabaseFactory.getThreadDatabase(getContext()).getThreadIdFor(Recipient.resolved(recipientId));

    if (gallery) return DatabaseFactory.getMediaDatabase(getContext()).getGalleryMediaForThread(threadId);
    else         return DatabaseFactory.getMediaDatabase(getContext()).getDocumentMediaForThread(threadId);
  }

  public RecipientId getRecipientId() {
    return recipientId;
  }

}
