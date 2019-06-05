package space.narrate.waylan.android.data.disk.wordset

import org.threeten.bp.OffsetDateTime


data class Synonym(
    val synonym: String,
    val created: OffsetDateTime,
    val modified: OffsetDateTime
)

