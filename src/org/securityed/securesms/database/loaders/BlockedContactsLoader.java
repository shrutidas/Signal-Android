package org.securityed.securesms.database.loaders;

import android.content.Context;
import android.database.Cursor;

import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext()).getBlocked();
  }

}
