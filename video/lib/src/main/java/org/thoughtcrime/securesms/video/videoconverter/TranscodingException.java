package org.thoughtcrime.securesms.ryan.video.videoconverter;

final class TranscodingException extends Exception {

  TranscodingException(String message) {
    super(message);
  }

  TranscodingException(Throwable inner) {
    super(inner);
  }

  TranscodingException(String message, Throwable inner) {
    super(message, inner);
  }
}
