package org.securityed.securesms.jobs;

import androidx.annotation.NonNull;

import org.securityed.securesms.crypto.ProfileKeyUtil;
import org.securityed.securesms.dependencies.ApplicationDependencies;
import org.securityed.securesms.jobmanager.Data;
import org.securityed.securesms.jobmanager.Job;
import org.securityed.securesms.jobmanager.impl.NetworkConstraint;
import org.securityed.securesms.logging.Log;
import org.securityed.securesms.service.IncomingMessageObserver;
import org.securityed.securesms.util.Base64;
import org.securityed.securesms.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessagePipe;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.crypto.ProfileCipher;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;

import java.io.IOException;

public class RefreshUnidentifiedDeliveryAbilityJob extends BaseJob {

  public static final String KEY = "RefreshUnidentifiedDeliveryAbilityJob";

  private static final String TAG = RefreshUnidentifiedDeliveryAbilityJob.class.getSimpleName();

  public RefreshUnidentifiedDeliveryAbilityJob() {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(10)
                           .build());
  }

  private RefreshUnidentifiedDeliveryAbilityJob(@NonNull Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull Data serialize() {
    return Data.EMPTY;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws Exception {
    byte[]               profileKey = ProfileKeyUtil.getProfileKey(context);
    SignalServiceProfile profile    = retrieveProfile(TextSecurePreferences.getLocalNumber(context));

    boolean enabled = profile.getUnidentifiedAccess() != null && isValidVerifier(profileKey, profile.getUnidentifiedAccess());

    TextSecurePreferences.setIsUnidentifiedDeliveryEnabled(context, enabled);
    Log.i(TAG, "Set UD status to: " + enabled);
  }

  @Override
  public void onCanceled() {
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception exception) {
    return exception instanceof PushNetworkException;
  }

  private SignalServiceProfile retrieveProfile(@NonNull String number) throws IOException {
    SignalServiceMessageReceiver receiver = ApplicationDependencies.getSignalServiceMessageReceiver();
    SignalServiceMessagePipe     pipe     = IncomingMessageObserver.getPipe();

    if (pipe != null) {
      try {
        return pipe.getProfile(new SignalServiceAddress(number), Optional.absent());
      } catch (IOException e) {
        Log.w(TAG, e);
      }
    }

    return receiver.retrieveProfile(new SignalServiceAddress(number), Optional.absent());
  }

  private boolean isValidVerifier(@NonNull byte[] profileKey, @NonNull String verifier) {
    ProfileCipher profileCipher = new ProfileCipher(profileKey);
    try {
      return profileCipher.verifyUnidentifiedAccess(Base64.decode(verifier));
    } catch (IOException e) {
      Log.w(TAG, e);
      return false;
    }
  }

  public static class Factory implements Job.Factory<RefreshUnidentifiedDeliveryAbilityJob> {
    @Override
    public @NonNull RefreshUnidentifiedDeliveryAbilityJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RefreshUnidentifiedDeliveryAbilityJob(parameters);
    }
  }
}
