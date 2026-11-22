package com.afbg.data.bgg

data class BggSearchHit(val bggId: Int, val name: String, val year: Int?)

data class BggGame(
    val bggId: Int,
    val name: String,
    val year: Int?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val playingTime: Int?,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val rating: Double?,
    val description: String?,
)

class BggException(message: String, val kind: Kind) : Exception(message) {
    enum class Kind { AUTH, RATE_LIMIT, NETWORK, SERVER, PARSE }
}
