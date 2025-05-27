package org.thoughtcrime.securesms.ryan.stories.settings.my

import org.thoughtcrime.securesms.ryan.database.model.DistributionListPrivacyMode

data class MyStoryPrivacyState(val privacyMode: DistributionListPrivacyMode? = null, val connectionCount: Int = 0)
