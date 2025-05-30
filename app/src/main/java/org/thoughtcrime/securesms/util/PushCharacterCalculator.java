/**
 * Copyright (C) 2015 Whisper Systems
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
package org.thoughtcrime.securesms.ryan.util;

import android.os.Parcel;

public class PushCharacterCalculator extends CharacterCalculator {
  private static final int MAX_TOTAL_SIZE   = 64 * 1024;
  private static final int MAX_PRIMARY_SIZE = 2000;
  @Override
  public CharacterState calculateCharacters(String messageBody) {
    return new CharacterState(1, MAX_TOTAL_SIZE - messageBody.length(), MAX_TOTAL_SIZE, MAX_PRIMARY_SIZE);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
  }

  public static final Creator<PushCharacterCalculator> CREATOR = new Creator<PushCharacterCalculator>() {
    @Override
    public PushCharacterCalculator createFromParcel(Parcel in) {
      return new PushCharacterCalculator();
    }

    @Override
    public PushCharacterCalculator[] newArray(int size) {
      return new PushCharacterCalculator[size];
    }
  };
}

