package org.thoughtcrime.securesms.ryan.video.videoconverter.muxer;

final class MuxingException extends RuntimeException {

  public MuxingException(String message) {
    super(message);
  }

  public MuxingException(String message, Throwable cause) {
    super(message, cause);
  }
}
