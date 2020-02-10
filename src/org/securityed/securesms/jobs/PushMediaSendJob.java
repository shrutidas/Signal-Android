package org.securityed.securesms.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import org.securityed.securesms.ApplicationContext;
import org.securityed.securesms.attachments.Attachment;
import org.securityed.securesms.crypto.UnidentifiedAccessUtil;
import org.securityed.securesms.database.Address;
import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.database.MessagingDatabase.SyncMessageId;
import org.securityed.securesms.database.MmsDatabase;
import org.securityed.securesms.database.NoSuchMessageException;
import org.securityed.securesms.database.RecipientDatabase.UnidentifiedAccessMode;
import org.securityed.securesms.dependencies.ApplicationDependencies;
import org.securityed.securesms.jobmanager.Data;
import org.securityed.securesms.jobmanager.Job;
import org.securityed.securesms.jobmanager.JobManager;
import org.securityed.securesms.logging.Log;
import org.securityed.securesms.mms.MmsException;
import org.securityed.securesms.mms.OutgoingMediaMessage;
import org.securityed.securesms.recipients.Recipient;
import org.securityed.securesms.service.ExpiringMessageManager;
import org.securityed.securesms.transport.InsecureFallbackApprovalException;
import org.securityed.securesms.transport.RetryLaterException;
import org.securityed.securesms.transport.UndeliverableMessageException;
import org.securityed.securesms.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Preview;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class PushMediaSendJob extends PushSendJob {

  public static final String KEY = "PushMediaSendJob";

  private static final String TAG = PushMediaSendJob.class.getSimpleName();

  private static final String KEY_MESSAGE_ID = "message_id";

  private long messageId;

  public PushMediaSendJob(long messageId, @NonNull Recipient recipient) {
    this(constructParameters(recipient), messageId);
  }

  private PushMediaSendJob(Job.Parameters parameters, long messageId) {
    super(parameters);
    this.messageId = messageId;
  }

  @WorkerThread
  public static void enqueue(@NonNull Context context, @NonNull JobManager jobManager, long messageId, @NonNull Recipient recipient) {
    try {
      if (!recipient.requireAddress().isPhone()) {
        throw new AssertionError();
      }

      MmsDatabase          database                    = DatabaseFactory.getMmsDatabase(context);
      OutgoingMediaMessage message                     = database.getOutgoingMessage(messageId);
      JobManager.Chain     compressAndUploadAttachment = createCompressingAndUploadAttachmentsChain(jobManager, message);

      compressAndUploadAttachment.then(new PushMediaSendJob(messageId, recipient))
                                 .enqueue();

    } catch (NoSuchMessageException | MmsException e) {
      Log.w(TAG, "Failed to enqueue message.", e);
      DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putLong(KEY_MESSAGE_ID, messageId).build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onAdded() {
    DatabaseFactory.getMmsDatabase(context).markAsSending(messageId);
  }

  @Override
  public void onPushSend()
      throws RetryLaterException, MmsException, NoSuchMessageException,
             UndeliverableMessageException
  {
    ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();
    MmsDatabase            database          = DatabaseFactory.getMmsDatabase(context);
    OutgoingMediaMessage   message           = database.getOutgoingMessage(messageId);

    if (database.isSent(messageId)) {
      warn(TAG, "Message " + messageId + " was already sent. Ignoring.");
      return;
    }

    try {
      log(TAG, "Sending message: " + messageId);

      Recipient              recipient  = message.getRecipient().resolve();
      byte[]                 profileKey = recipient.getProfileKey();
      UnidentifiedAccessMode accessMode = recipient.getUnidentifiedAccessMode();

      boolean unidentified = deliver(message);

      database.markAsSent(messageId, true);
      markAttachmentsUploaded(messageId, message.getAttachments());
      database.markUnidentified(messageId, unidentified);

      if (recipient.isLocalNumber()) {
        SyncMessageId id = new SyncMessageId(recipient.getId(), message.getSentTimeMillis());
        DatabaseFactory.getMmsSmsDatabase(context).incrementDeliveryReceiptCount(id, System.currentTimeMillis());
        DatabaseFactory.getMmsSmsDatabase(context).incrementReadReceiptCount(id, System.currentTimeMillis());
      }

      if (TextSecurePreferences.isUnidentifiedDeliveryEnabled(context)) {
        if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN && profileKey == null) {
          log(TAG, "Marking recipient as UD-unrestricted following a UD send.");
          DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient.getId(), UnidentifiedAccessMode.UNRESTRICTED);
        } else if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN) {
          log(TAG, "Marking recipient as UD-enabled following a UD send.");
          DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient.getId(), UnidentifiedAccessMode.ENABLED);
        } else if (!unidentified && accessMode != UnidentifiedAccessMode.DISABLED) {
          log(TAG, "Marking recipient as UD-disabled following a non-UD send.");
          DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient.getId(), UnidentifiedAccessMode.DISABLED);
        }
      }

      if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
        database.markExpireStarted(messageId);
        expirationManager.scheduleDeletion(messageId, true, message.getExpiresIn());
      }

      if (message.isViewOnce()) {
        DatabaseFactory.getAttachmentDatabase(context).deleteAttachmentFilesForMessage(messageId);
      }

      log(TAG, "Sent message: " + messageId);

    } catch (InsecureFallbackApprovalException ifae) {
      warn(TAG, "Failure", ifae);
      database.markAsPendingInsecureSmsFallback(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
      ApplicationContext.getInstance(context).getJobManager().add(new DirectoryRefreshJob(false));
    } catch (UntrustedIdentityException uie) {
      warn(TAG, "Failure", uie);
      database.addMismatchedIdentity(messageId, Recipient.external(context, uie.getE164Number()).getId(), uie.getIdentityKey());
      database.markAsSentFailed(messageId);
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof RetryLaterException) return true;
    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
    notifyMediaMessageDeliveryFailed(context, messageId);
  }

  private boolean deliver(OutgoingMediaMessage message)
      throws RetryLaterException, InsecureFallbackApprovalException, UntrustedIdentityException,
             UndeliverableMessageException
  {
    if (message.getRecipient() == null) {
      throw new UndeliverableMessageException("No destination address.");
    }

    final Address destination = message.getRecipient().requireAddress();

    if (!destination.isPhone()) {
      if (destination.isEmail()) throw new UndeliverableMessageException("Not e164, is email");
      if (destination.isGroup()) throw new UndeliverableMessageException("Not e164, is group");
      throw new UndeliverableMessageException("Not e164, unknown");
    }

    try {
      rotateSenderCertificateIfNecessary();

      SignalServiceMessageSender                 messageSender      = ApplicationDependencies.getSignalServiceMessageSender();
      SignalServiceAddress                       address            = getPushAddress(destination);
      List<Attachment>                           attachments        = Stream.of(message.getAttachments()).filterNot(Attachment::isSticker).toList();
      List<SignalServiceAttachment>              serviceAttachments = getAttachmentPointersFor(attachments);
      Optional<byte[]>                           profileKey         = getProfileKey(message.getRecipient());
      Optional<SignalServiceDataMessage.Quote>   quote              = getQuoteFor(message);
      Optional<SignalServiceDataMessage.Sticker> sticker            = getStickerFor(message);
      List<SharedContact>                        sharedContacts     = getSharedContactsFor(message);
      List<Preview>                              previews           = getPreviewsFor(message);
      SignalServiceDataMessage                   mediaMessage       = SignalServiceDataMessage.newBuilder()
                                                                                            .withBody(message.getBody())
                                                                                            .withAttachments(serviceAttachments)
                                                                                            .withTimestamp(message.getSentTimeMillis())
                                                                                            .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                                            .withViewOnce(message.isViewOnce())
                                                                                            .withProfileKey(profileKey.orNull())
                                                                                            .withQuote(quote.orNull())
                                                                                            .withSticker(sticker.orNull())
                                                                                            .withSharedContacts(sharedContacts)
                                                                                            .withPreviews(previews)
                                                                                            .asExpirationUpdate(message.isExpirationUpdate())
                                                                                            .build();

      if (address.getNumber().equals(TextSecurePreferences.getLocalNumber(context))) {
        Optional<UnidentifiedAccessPair> syncAccess  = UnidentifiedAccessUtil.getAccessForSync(context);
        SignalServiceSyncMessage         syncMessage = buildSelfSendSyncMessage(context, mediaMessage, syncAccess);

        messageSender.sendMessage(syncMessage, syncAccess);
        return syncAccess.isPresent();
      } else {
        return messageSender.sendMessage(address, UnidentifiedAccessUtil.getAccessFor(context, message.getRecipient()), mediaMessage).getSuccess().isUnidentified();
      }
    } catch (UnregisteredUserException e) {
      warn(TAG, e);
      throw new InsecureFallbackApprovalException(e);
    } catch (FileNotFoundException e) {
      warn(TAG, e);
      throw new UndeliverableMessageException(e);
    } catch (IOException e) {
      warn(TAG, e);
      throw new RetryLaterException(e);
    }
  }

  public static final class Factory implements Job.Factory<PushMediaSendJob> {
    @Override
    public @NonNull PushMediaSendJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new PushMediaSendJob(parameters, data.getLong(KEY_MESSAGE_ID));
    }
  }
}