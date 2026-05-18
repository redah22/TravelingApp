package com.traveling.app.travelshare.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.traveling.app.R
import com.traveling.app.travelshare.models.PhotoPost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bumptech.glide.Glide

class PhotoPostAdapter(
    private var posts: List<PhotoPost>,
    private val onLikeClicked: (PhotoPost) -> Unit,
    private val onCommentClicked: (PhotoPost) -> Unit,
    private val onPostClicked: (PhotoPost) -> Unit
) : RecyclerView.Adapter<PhotoPostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatarInitials: TextView = view.findViewById(R.id.tvAvatarInitials)
        val tvAuthorName: TextView = view.findViewById(R.id.tvAuthorName)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val btnLike: View = view.findViewById(R.id.btnLike)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val btnComment: View = view.findViewById(R.id.btnComment)
        val tvCommentsCount: TextView = view.findViewById(R.id.tvCommentsCount)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val name = post.autheur.nomComplet
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

        holder.tvAvatarInitials.text = initials
        holder.tvAuthorName.text = name
        holder.tvDate.text = formatDateRelative(post.datePublicationMillis)
        holder.tvLikesCount.text = post.likesCount.toString()
        holder.tvCommentsCount.text = post.commentsCount.toString()
        holder.tvLocation.text = "📍 ${post.lieuNom}"
        holder.tvDescription.text = post.descriptionText

        // Accessibilité TalkBack
        holder.itemView.contentDescription = "Photo de $name à ${post.lieuNom}. ${post.likesCount} j'aime."

        if (post.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.photoUrl)
                .placeholder(R.drawable.placeholder_post)
                .error(R.drawable.placeholder_post)
                .centerCrop()
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(R.drawable.placeholder_post)
        }
        holder.ivPhoto.contentDescription = "Photo de voyage à ${post.lieuNom}"

        holder.itemView.setOnClickListener { onPostClicked(post) }
        holder.btnLike.setOnClickListener { onLikeClicked(post) }
        holder.btnComment.setOnClickListener { onCommentClicked(post) }
    }

    override fun getItemCount() = posts.size

    private fun formatDateRelative(millis: Long): String {
        val diffMs = System.currentTimeMillis() - millis
        val diffMin = diffMs / 60_000
        val diffH = diffMs / 3_600_000
        val diffD = diffMs / 86_400_000
        return when {
            diffMin < 1  -> "À l'instant"
            diffMin < 60 -> "Il y a ${diffMin}min"
            diffH < 24   -> "Il y a ${diffH}h"
            diffD == 1L  -> "Hier"
            diffD < 7    -> "Il y a ${diffD}j"
            else         -> SimpleDateFormat("d MMM yyyy", Locale.FRENCH).format(Date(millis))
        }
    }

    fun updateData(newPosts: List<PhotoPost>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
