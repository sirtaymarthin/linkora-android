package com.linkora.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Traducción de la clave textual de icono (categorías y marcas) a vector. */
object Glyphs {

    fun byKey(key: String?): ImageVector = when (key) {
        "play" -> Icons.Filled.PlayArrow
        "camera" -> Icons.Outlined.PhotoCamera
        "chat" -> Icons.Outlined.ChatBubbleOutline
        "public" -> Icons.Outlined.Public
        "work" -> Icons.Outlined.WorkOutline
        "music" -> Icons.Outlined.MusicNote
        "code" -> Icons.Outlined.Code
        "forum" -> Icons.Outlined.Forum
        "videocam" -> Icons.Outlined.Videocam
        "book" -> Icons.Outlined.MenuBook
        "map" -> Icons.Outlined.Map
        "cart" -> Icons.Outlined.ShoppingCart
        "hotel" -> Icons.Outlined.Hotel
        "pdf" -> Icons.Outlined.PictureAsPdf
        "image" -> Icons.Outlined.Image
        "file" -> Icons.Outlined.InsertDriveFile
        "movie" -> Icons.Outlined.Movie
        "run" -> Icons.Outlined.DirectionsRun
        "food" -> Icons.Outlined.Restaurant
        "plane" -> Icons.Outlined.Flight
        "school" -> Icons.Outlined.School
        "home" -> Icons.Outlined.Home
        "star" -> Icons.Outlined.StarOutline
        "heart" -> Icons.Outlined.FavoriteBorder
        "money" -> Icons.Outlined.Savings
        "photo" -> Icons.Outlined.PhotoLibrary
        "idea" -> Icons.Outlined.Lightbulb
        "game" -> Icons.Outlined.SportsEsports
        "pet" -> Icons.Outlined.Pets
        "news" -> Icons.Outlined.Newspaper
        else -> Icons.Outlined.Link
    }

    /** Iconos disponibles al crear o editar una categoría. */
    val pickable = listOf(
        "movie", "music", "book", "work", "run", "food", "plane", "school",
        "home", "cart", "money", "photo", "idea", "game", "pet", "news",
        "map", "hotel", "code", "star"
    )

    val palette = listOf(
        0xFF5E5CE6, 0xFFE5484D, 0xFFF5A524, 0xFF30A46C, 0xFF0EA5E9,
        0xFFEC4899, 0xFF8B5CF6, 0xFFF97316, 0xFF14B8A6, 0xFF64748B
    )
}
