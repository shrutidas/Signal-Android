package org.securityed.securesms.jobs;

import androidx.annotation.NonNull;

import org.securityed.securesms.BuildConfig;
import org.securityed.securesms.TextSecureExpiredException;
import org.securityed.securesms.attachments.Attachment;
import org.securityed.securesms.database.AttachmentDatabase;
import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.jobmanager.Job;
import org.securityed.securesms.logging.Log;
import org.securityed.securesms.util.Util;

import java.util.List;

public abstract class SendJob extends BaseJob {

  @SuppressWarnings("unused")
  private final static String TAG = SendJob.class.getSimpleName();

  public SendJob(Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public final void onRun() throws Exception {
    if (Util.getDaysTillBuildExpiry() <= 0) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    Log.i(TAG, "Starting message send attempt");
    onSend();
    Log.i(TAG, "Message send completed");
  }

  protected abstract void onSend() throws Exception;

  protected void markAttachmentsUploaded(long messageId, @NonNull List<Attachment> attachments) {
    AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }
}
