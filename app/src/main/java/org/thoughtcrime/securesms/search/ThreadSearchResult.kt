package org.thoughtcrime.securesms.ryan.search

import org.thoughtcrime.securesms.ryan.database.model.ThreadRecord

data class ThreadSearchResult(val results: List<ThreadRecord>, val query: String)
