package org.securityed.securesms.sms;

public class IncomingIdentityUpdateMessage extends IncomingTextMessage {

  public IncomingIdentityUpdateMessage(IncomingTextMessage base) {
    super(base, "");
  }

  @Override
  public boolean isIdentityUpdate() {
    return true;
  }

}
