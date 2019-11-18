package org.securityed.securesms.jobmanager.workmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.securityed.securesms.jobs.AttachmentDownloadJob;
import org.securityed.securesms.jobs.AttachmentUploadJob;
import org.securityed.securesms.jobs.AvatarDownloadJob;
import org.securityed.securesms.jobs.CleanPreKeysJob;
import org.securityed.securesms.jobs.CreateSignedPreKeyJob;
import org.securityed.securesms.jobs.DirectoryRefreshJob;
import org.securityed.securesms.jobs.FailingJob;
import org.securityed.securesms.jobs.FcmRefreshJob;
import org.securityed.securesms.jobs.LocalBackupJob;
import org.securityed.securesms.jobs.MmsDownloadJob;
import org.securityed.securesms.jobs.MmsReceiveJob;
import org.securityed.securesms.jobs.MmsSendJob;
import org.securityed.securesms.jobs.MultiDeviceBlockedUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceConfigurationUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceContactUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceGroupUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceProfileKeyUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceReadUpdateJob;
import org.securityed.securesms.jobs.MultiDeviceVerifiedUpdateJob;
import org.securityed.securesms.jobs.PushDecryptJob;
import org.securityed.securesms.jobs.PushGroupSendJob;
import org.securityed.securesms.jobs.PushGroupUpdateJob;
import org.securityed.securesms.jobs.PushMediaSendJob;
import org.securityed.securesms.jobs.PushNotificationReceiveJob;
import org.securityed.securesms.jobs.PushTextSendJob;
import org.securityed.securesms.jobs.RefreshAttributesJob;
import org.securityed.securesms.jobs.RefreshPreKeysJob;
import org.securityed.securesms.jobs.RefreshUnidentifiedDeliveryAbilityJob;
import org.securityed.securesms.jobs.RequestGroupInfoJob;
import org.securityed.securesms.jobs.RetrieveProfileAvatarJob;
import org.securityed.securesms.jobs.RetrieveProfileJob;
import org.securityed.securesms.jobs.RotateCertificateJob;
import org.securityed.securesms.jobs.RotateProfileKeyJob;
import org.securityed.securesms.jobs.RotateSignedPreKeyJob;
import org.securityed.securesms.jobs.SendDeliveryReceiptJob;
import org.securityed.securesms.jobs.SendReadReceiptJob;
import org.securityed.securesms.jobs.ServiceOutageDetectionJob;
import org.securityed.securesms.jobs.SmsReceiveJob;
import org.securityed.securesms.jobs.SmsSendJob;
import org.securityed.securesms.jobs.SmsSentJob;
import org.securityed.securesms.jobs.TrimThreadJob;
import org.securityed.securesms.jobs.TypingSendJob;
import org.securityed.securesms.jobs.UpdateApkJob;

import java.util.HashMap;
import java.util.Map;

public class WorkManagerFactoryMappings {

  private static final Map<String, String> FACTORY_MAP = new HashMap<String, String>() {{
    put(AttachmentDownloadJob.class.getName(), AttachmentDownloadJob.KEY);
    put(AttachmentUploadJob.class.getName(), AttachmentUploadJob.KEY);
    put(AvatarDownloadJob.class.getName(), AvatarDownloadJob.KEY);
    put(CleanPreKeysJob.class.getName(), CleanPreKeysJob.KEY);
    put(CreateSignedPreKeyJob.class.getName(), CreateSignedPreKeyJob.KEY);
    put(DirectoryRefreshJob.class.getName(), DirectoryRefreshJob.KEY);
    put(FcmRefreshJob.class.getName(), FcmRefreshJob.KEY);
    put(LocalBackupJob.class.getName(), LocalBackupJob.KEY);
    put(MmsDownloadJob.class.getName(), MmsDownloadJob.KEY);
    put(MmsReceiveJob.class.getName(), MmsReceiveJob.KEY);
    put(MmsSendJob.class.getName(), MmsSendJob.KEY);
    put(MultiDeviceBlockedUpdateJob.class.getName(), MultiDeviceBlockedUpdateJob.KEY);
    put(MultiDeviceConfigurationUpdateJob.class.getName(), MultiDeviceConfigurationUpdateJob.KEY);
    put(MultiDeviceContactUpdateJob.class.getName(), MultiDeviceContactUpdateJob.KEY);
    put(MultiDeviceGroupUpdateJob.class.getName(), MultiDeviceGroupUpdateJob.KEY);
    put(MultiDeviceProfileKeyUpdateJob.class.getName(), MultiDeviceProfileKeyUpdateJob.KEY);
    put(MultiDeviceReadUpdateJob.class.getName(), MultiDeviceReadUpdateJob.KEY);
    put(MultiDeviceVerifiedUpdateJob.class.getName(), MultiDeviceVerifiedUpdateJob.KEY);
    put("PushContentReceiveJob", FailingJob.KEY);
    put(PushDecryptJob.class.getName(), PushDecryptJob.KEY);
    put(PushGroupSendJob.class.getName(), PushGroupSendJob.KEY);
    put(PushGroupUpdateJob.class.getName(), PushGroupUpdateJob.KEY);
    put(PushMediaSendJob.class.getName(), PushMediaSendJob.KEY);
    put(PushNotificationReceiveJob.class.getName(), PushNotificationReceiveJob.KEY);
    put(PushTextSendJob.class.getName(), PushTextSendJob.KEY);
    put(RefreshAttributesJob.class.getName(), RefreshAttributesJob.KEY);
    put(RefreshPreKeysJob.class.getName(), RefreshPreKeysJob.KEY);
    put(RefreshUnidentifiedDeliveryAbilityJob.class.getName(), RefreshUnidentifiedDeliveryAbilityJob.KEY);
    put(RequestGroupInfoJob.class.getName(), RequestGroupInfoJob.KEY);
    put(RetrieveProfileAvatarJob.class.getName(), RetrieveProfileAvatarJob.KEY);
    put(RetrieveProfileJob.class.getName(), RetrieveProfileJob.KEY);
    put(RotateCertificateJob.class.getName(), RotateCertificateJob.KEY);
    put(RotateProfileKeyJob.class.getName(), RotateProfileKeyJob.KEY);
    put(RotateSignedPreKeyJob.class.getName(), RotateSignedPreKeyJob.KEY);
    put(SendDeliveryReceiptJob.class.getName(), SendDeliveryReceiptJob.KEY);
    put(SendReadReceiptJob.class.getName(), SendReadReceiptJob.KEY);
    put(ServiceOutageDetectionJob.class.getName(), ServiceOutageDetectionJob.KEY);
    put(SmsReceiveJob.class.getName(), SmsReceiveJob.KEY);
    put(SmsSendJob.class.getName(), SmsSendJob.KEY);
    put(SmsSentJob.class.getName(), SmsSentJob.KEY);
    put(TrimThreadJob.class.getName(), TrimThreadJob.KEY);
    put(TypingSendJob.class.getName(), TypingSendJob.KEY);
    put(UpdateApkJob.class.getName(), UpdateApkJob.KEY);
  }};

  public static @Nullable String getFactoryKey(@NonNull String workManagerClass) {
    return FACTORY_MAP.get(workManagerClass);
  }
}
