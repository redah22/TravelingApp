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
        holder.tvDate.text = formatDate(post.datePublicationMillis)
        holder.tvLikesCount.text = post.likesCount.toString()
        holder.tvCommentsCount.text = post.commentsCount.toString()
        holder.tvLocation.text = post.lieuNom

        if (post.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.itemView.setOnClickListener { onPostClicked(post) }
        holder.btnLike.setOnClickListener { onLikeClicked(post) }
        holder.btnComment.setOnClickListener { onCommentClicked(post) }
    }

    override fun getItemCount() = posts.size

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale.FRENCH)
        return sdf.format(Date(millis))
    }

    fun updateData(newPosts: List<PhotoPost>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
