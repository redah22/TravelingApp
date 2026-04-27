package com.traveling.app.travelshare.models

data class User(
    val id: String,
    val nomComplet: String,
    val pseudo: String,
    val avatarUrl: String,
    val isAnonymous: Boolean = false
)
