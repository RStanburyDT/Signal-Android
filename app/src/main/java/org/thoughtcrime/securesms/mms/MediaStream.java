/**
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.ryan.mms;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MediaStream implements Closeable {
  private final InputStream stream;
  private final String      mimeType;
  private final int         width;
  private final int         height;
  private final boolean     faststart;

  public MediaStream(InputStream stream, String mimeType, int width, int height) {
    this.stream    = stream;
    this.mimeType  = mimeType;
    this.width     = width;
    this.height    = height;
    this.faststart = false;
  }

  public MediaStream(InputStream stream, String mimeType, int width, int height, boolean faststart) {
    this.stream    = stream;
    this.mimeType  = mimeType;
    this.width     = width;
    this.height    = height;
    this.faststart = faststart;
  }

  public InputStream getStream() {
    return stream;
  }

  public String getMimeType() {
    return mimeType;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean getFaststart() {
    return faststart;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }
}
