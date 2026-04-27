package com.traveling.app.travelshare.models

data class PhotoPost(
    val id: String,
    val autheur: User,
    val photoUrl: String,
    val descriptionText: String,
    val audioUrl: String? = null,
    val iaTags: List<String> = emptyList(),
    val lieuNom: String,
    val latitude: Double,
    val longitude: Double,
    val datePublicationMillis: Long,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val scope: ShareScope = ShareScope.PUBLIC
)

enum class ShareScope {
    PUBLIC,
    PRIVATE_GROUP,
    PRIVATE
}
