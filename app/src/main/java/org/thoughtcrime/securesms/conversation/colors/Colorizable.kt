package org.thoughtcrime.securesms.ryan.conversation.colors

import android.view.ViewGroup
import org.thoughtcrime.securesms.ryan.util.ProjectionList

/**
 * Denotes that a class can be colorized. The class is responsible for
 * generating its own projection.
 */
interface Colorizable {
  fun getColorizerProjections(coordinateRoot: ViewGroup): ProjectionList
}
