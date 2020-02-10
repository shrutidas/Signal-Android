package org.securityed.securesms.events;

import androidx.annotation.NonNull;

import org.securityed.securesms.attachments.Attachment;

public final class PartProgressEvent {

  public final Attachment attachment;
  public final Type       type;
  public final long       total;
  public final long       progress;

  public enum Type {
    COMPRESSION,
    NETWORK
  }

  public PartProgressEvent(@NonNull Attachment attachment, @NonNull Type type, long total, long progress) {
    this.attachment = attachment;
    this.type       = type;
    this.total      = total;
    this.progress   = progress;
  }
}