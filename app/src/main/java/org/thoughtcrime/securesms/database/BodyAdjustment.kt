package org.thoughtcrime.securesms.ryan.database

/**
 * Store data about an operation that changes the contents of a body.
 */
data class BodyAdjustment(val startIndex: Int, val oldLength: Int, val newLength: Int)
