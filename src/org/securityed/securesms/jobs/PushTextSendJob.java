package org.securityed.securesms.jobs;

import androidx.annotation.NonNull;

import org.securityed.securesms.database.MessagingDatabase.SyncMessageId;
import org.securityed.securesms.database.RecipientDatabase.UnidentifiedAccessMode;

import org.securityed.securesms.ApplicationContext;
import org.securityed.securesms.crypto.UnidentifiedAccessUtil;
import org.securityed.securesms.database.DatabaseFactory;
import org.securityed.securesms.database.NoSuchMessageException;
import org.securityed.securesms.database.SmsDatabase;
import org.securityed.securesms.database.model.SmsMessageRecord;
import org.securityed.securesms.dependencies.ApplicationDependencies;
import org.securityed.securesms.jobmanager.Data;
import org.securityed.securesms.jobmanager.Job;
import org.securityed.securesms.notifications.MessageNotifier;
import org.securityed.securesms.recipients.Recipient;
import org.securityed.securesms.service.ExpiringMessageManager;
import org.securityed.securesms.transport.InsecureFallbackApprovalException;
import org.securityed.securesms.transport.RetryLaterException;
import org.securityed.securesms.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.multidevice.SignalServiceSyncMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.io.IOException;

public class PushTextSendJob extends PushSendJob {

  public static final String KEY = "PushTextSendJob";

  private static final String TAG = PushTextSendJob.class.getSimpleName();

  private static final String KEY_MESSAGE_ID = "message_id";

  private long messageId;

  public PushTextSendJob(long messageId, @NonNull Recipient recipient) {
    this(constructParameters(recipient), messageId);
  }

  private PushTextSendJob(@NonNull Job.Parameters parameters, long messageId) {
    super(parameters);
    this.messageId = messageId;
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
    DatabaseFactory.getSmsDatabase(context).markAsSending(messageId);
  }

  @Override
  public void onPushSend() throws NoSuchMessageException, RetryLaterException {
    ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();
    SmsDatabase            database          = DatabaseFactory.getSmsDatabase(context);
    SmsMessageRecord       record            = database.getMessage(messageId);

    if (!record.isPending() && !record.isFailed()) {
      warn(TAG, "Message " + messageId + " was already sent. Ignoring.");
      return;
    }

    try {
      log(TAG, "Sending message: " + messageId);

      Recipient              recipient  = record.getRecipient().resolve();
      byte[]                 profileKey = recipient.getProfileKey();
      UnidentifiedAccessMode accessMode = recipient.getUnidentifiedAccessMode();

      boolean unidentified = deliver(record);

      database.markAsSent(messageId, true);
      database.markUnidentified(messageId, unidentified);

      if (recipient.isLocalNumber()) {
        SyncMessageId id = new SyncMessageId(recipient.getId(), record.getDateSent());
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

      if (record.getExpiresIn() > 0) {
        database.markExpireStarted(messageId);
        expirationManager.scheduleDeletion(record.getId(), record.isMms(), record.getExpiresIn());
      }

      log(TAG, "Sent message: " + messageId);

    } catch (InsecureFallbackApprovalException e) {
      warn(TAG, "Failure", e);
      database.markAsPendingInsecureSmsFallback(record.getId());
      MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
      ApplicationContext.getInstance(context).getJobManager().add(new DirectoryRefreshJob(false));
    } catch (UntrustedIdentityException e) {
      warn(TAG, "Failure", e);
      database.addMismatchedIdentity(record.getId(), Recipient.external(context, e.getE164Number()).getId(), e.getIdentityKey());
      database.markAsSentFailed(record.getId());
      database.markAsPush(record.getId());
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof RetryLaterException) return true;

    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);

    long      threadId  = DatabaseFactory.getSmsDatabase(context).getThreadIdForMessage(messageId);
    Recipient recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadId);

    if (threadId != -1 && recipient != null) {
      MessageNotifier.notifyMessageDeliveryFailed(context, recipient, threadId);
    }
  }

  private boolean deliver(SmsMessageRecord message)
      throws UntrustedIdentityException, InsecureFallbackApprovalException, RetryLaterException
  {
    try {
      rotateSenderCertificateIfNecessary();

      SignalServiceMessageSender       messageSender      = ApplicationDependencies.getSignalServiceMessageSender();
      SignalServiceAddress             address            = getPushAddress(message.getIndividualRecipient().requireAddress());
      Optional<byte[]>                 profileKey         = getProfileKey(message.getIndividualRecipient());
      Optional<UnidentifiedAccessPair> unidentifiedAccess = UnidentifiedAccessUtil.getAccessFor(context, message.getIndividualRecipient());

      log(TAG, "Have access key to use: " + unidentifiedAccess.isPresent());

      SignalServiceDataMessage textSecureMessage = SignalServiceDataMessage.newBuilder()
                                                                           .withTimestamp(message.getDateSent())
                                                                           .withBody(message.getBody())
                                                                           .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                           .withProfileKey(profileKey.orNull())
                                                                           .asEndSessionMessage(message.isEndSession())
                                                                           .build();

      if (address.getNumber().equals(TextSecurePreferences.getLocalNumber(context))) {
        Optional<UnidentifiedAccessPair> syncAccess  = UnidentifiedAccessUtil.getAccessForSync(context);
        SignalServiceSyncMessage         syncMessage = buildSelfSendSyncMessage(context, textSecureMessage, syncAccess);

        messageSender.sendMessage(syncMessage, syncAccess);
        return syncAccess.isPresent();
      } else {
        return messageSender.sendMessage(address, unidentifiedAccess, textSecureMessage).getSuccess().isUnidentified();
      }
    } catch (UnregisteredUserException e) {
      warn(TAG, "Failure", e);
      throw new InsecureFallbackApprovalException(e);
    } catch (IOException e) {
      warn(TAG, "Failure", e);
      throw new RetryLaterException(e);
    }
  }

  public static class Factory implements Job.Factory<PushTextSendJob> {
    @Override
    public @NonNull PushTextSendJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new PushTextSendJob(parameters, data.getLong(KEY_MESSAGE_ID));
    }
  }
}