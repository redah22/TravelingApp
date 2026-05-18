package com.traveling.app.travelshare.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.traveling.app.travelshare.models.PhotoPost
import com.traveling.app.travelshare.models.ShareScope
import com.traveling.app.travelshare.models.User

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "travelshare.db"
        private const val DB_VERSION = 5 // v5: Added groups & group_members tables

        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"

        const val TABLE_POSTS = "posts"
        const val COL_POST_ID = "post_id"
        const val COL_POST_AUTHOR = "author_name"
        const val COL_POST_PHOTO_URL = "photo_url"
        const val COL_POST_DESC = "description"
        const val COL_POST_LOCATION = "location"
        const val COL_POST_LAT = "latitude"
        const val COL_POST_LNG = "longitude"
        const val COL_POST_SCOPE = "scope"
        const val COL_POST_DATE = "date"
        const val COL_POST_LIKES = "likes"
        const val COL_POST_COMMENTS = "comments"

        const val TABLE_LIKES = "likes"
        const val COL_LIKE_ID = "like_id"
        const val COL_LIKE_USER = "user_name"
        const val COL_LIKE_POST_ID = "post_id"

        const val TABLE_COMMENTS = "comments"
        const val COL_CMT_ID = "cmt_id"
        const val COL_CMT_POST_ID = "post_id"
        const val COL_CMT_USER = "user_name"
        const val COL_CMT_TEXT = "text"
        const val COL_CMT_DATE = "date"

        // --- GROUPS ---
        const val TABLE_GROUPS = "groups_tbl"
        const val COL_GRP_ID = "grp_id"
        const val COL_GRP_NAME = "grp_name"
        const val COL_GRP_DESC = "grp_desc"
        const val COL_GRP_CREATOR = "grp_creator"
        const val COL_GRP_DATE = "grp_date"

        const val TABLE_GROUP_MEMBERS = "group_members"
        const val COL_GM_ID = "gm_id"
        const val COL_GM_GROUP = "gm_group_id"
        const val COL_GM_USER = "gm_user_name"

        // --- FOLLOWS ---
        const val TABLE_FOLLOWS = "follows"
        const val COL_FOLLOW_FOLLOWER = "follower"
        const val COL_FOLLOW_FOLLOWED = "followed"

        @Volatile
        private var instance: DatabaseHelper? = null

        @JvmStatic
        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT UNIQUE NOT NULL,
                $COL_PASSWORD TEXT NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_POSTS (
                $COL_POST_ID TEXT PRIMARY KEY,
                $COL_POST_AUTHOR TEXT NOT NULL,
                $COL_POST_PHOTO_URL TEXT NOT NULL,
                $COL_POST_DESC TEXT NOT NULL,
                $COL_POST_LOCATION TEXT NOT NULL,
                $COL_POST_LAT REAL NOT NULL,
                $COL_POST_LNG REAL NOT NULL,
                $COL_POST_SCOPE TEXT NOT NULL,
                $COL_POST_DATE INTEGER NOT NULL,
                $COL_POST_LIKES INTEGER NOT NULL,
                $COL_POST_COMMENTS INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_LIKES (
                $COL_LIKE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LIKE_USER TEXT NOT NULL,
                $COL_LIKE_POST_ID TEXT NOT NULL,
                UNIQUE($COL_LIKE_USER, $COL_LIKE_POST_ID)
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_COMMENTS (
                $COL_CMT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CMT_POST_ID TEXT NOT NULL,
                $COL_CMT_USER TEXT NOT NULL,
                $COL_CMT_TEXT TEXT NOT NULL,
                $COL_CMT_DATE INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_GROUPS (
                $COL_GRP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GRP_NAME TEXT NOT NULL,
                $COL_GRP_DESC TEXT,
                $COL_GRP_CREATOR TEXT NOT NULL,
                $COL_GRP_DATE INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_GROUP_MEMBERS (
                $COL_GM_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GM_GROUP INTEGER NOT NULL,
                $COL_GM_USER TEXT NOT NULL,
                UNIQUE($COL_GM_GROUP, $COL_GM_USER)
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE_FOLLOWS (
                $COL_FOLLOW_FOLLOWER TEXT NOT NULL,
                $COL_FOLLOW_FOLLOWED TEXT NOT NULL,
                PRIMARY KEY($COL_FOLLOW_FOLLOWER, $COL_FOLLOW_FOLLOWED)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LIKES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COMMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GROUP_MEMBERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLLOWS")
        onCreate(db)
    }

    // --- USERS ---
    fun insererUtilisateur(name: String, email: String, password: String): Long {
        val values = ContentValues().apply {
            put(COL_NAME, name)
            put(COL_EMAIL, email)
            put(COL_PASSWORD, password)
        }
        return writableDatabase.insert(TABLE_USERS, null, values)
    }

    fun emailExiste(email: String): Boolean {
        val cursor = readableDatabase.query(
            TABLE_USERS, arrayOf(COL_ID),
            "$COL_EMAIL = ?", arrayOf(email),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun verifierConnexion(email: String, password: String): String? {
        val cursor = readableDatabase.query(
            TABLE_USERS, arrayOf(COL_NAME),
            "$COL_EMAIL = ? AND $COL_PASSWORD = ?", arrayOf(email, password),
            null, null, null
        )
        val name = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()
        return name
    }

    // --- POSTS ---
    fun insererPost(post: PhotoPost): Long {
        val values = ContentValues().apply {
            put(COL_POST_ID, post.id)
            put(COL_POST_AUTHOR, post.autheur.nomComplet)
            put(COL_POST_PHOTO_URL, post.photoUrl)
            put(COL_POST_DESC, post.descriptionText)
            put(COL_POST_LOCATION, post.lieuNom)
            put(COL_POST_LAT, post.latitude)
            put(COL_POST_LNG, post.longitude)
            put(COL_POST_SCOPE, post.scope.name)
            put(COL_POST_DATE, post.datePublicationMillis)
            put(COL_POST_LIKES, post.likesCount)
            put(COL_POST_COMMENTS, post.commentsCount)
        }
        return writableDatabase.insert(TABLE_POSTS, null, values)
    }

    fun recupererTousLesPosts(): List<PhotoPost> {
        val posts = mutableListOf<PhotoPost>()
        val cursor = readableDatabase.query(
            TABLE_POSTS, null, null, null, null, null, "$COL_POST_DATE DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                posts.add(cursorToPost(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return posts
    }

    /** Retourne uniquement les posts visibles dans le fil public (scope = PUBLIC) */
    fun recupererPostsPublics(): List<PhotoPost> {
        val posts = mutableListOf<PhotoPost>()
        val cursor = readableDatabase.query(
            TABLE_POSTS, null,
            "$COL_POST_SCOPE = ?", arrayOf(ShareScope.PUBLIC.name),
            null, null, "$COL_POST_DATE DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                posts.add(cursorToPost(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return posts
    }

    /** Retourne les posts d'un utilisateur (tous scopes confondus pour son propre profil) */
    fun recupererPostsParUtilisateur(userName: String): List<PhotoPost> {
        val posts = mutableListOf<PhotoPost>()
        val cursor = readableDatabase.query(
            TABLE_POSTS, null,
            "$COL_POST_AUTHOR = ?", arrayOf(userName),
            null, null, "$COL_POST_DATE DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                posts.add(cursorToPost(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return posts
    }


    private fun cursorToPost(cursor: android.database.Cursor): PhotoPost {
        val scopeStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_SCOPE))
        val scope = try { ShareScope.valueOf(scopeStr) } catch(e: Exception) { ShareScope.PUBLIC }
        
        return PhotoPost(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_ID)),
            autheur = User("u", cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_AUTHOR)), "@", ""),
            photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_PHOTO_URL)),
            descriptionText = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_DESC)),
            lieuNom = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_LOCATION)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_POST_LAT)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_POST_LNG)),
            scope = scope,
            datePublicationMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_POST_DATE)),
            likesCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_LIKES)),
            commentsCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_COMMENTS))
        )
    }

    // --- LIKES ---
    fun toggleLike(userName: String, postId: String): Boolean {
        val db = writableDatabase
        val cursor = db.query(TABLE_LIKES, null, "$COL_LIKE_USER = ? AND $COL_LIKE_POST_ID = ?", arrayOf(userName, postId), null, null, null)
        val alreadyLiked = cursor.count > 0
        cursor.close()

        if (alreadyLiked) {
            db.delete(TABLE_LIKES, "$COL_LIKE_USER = ? AND $COL_LIKE_POST_ID = ?", arrayOf(userName, postId))
            // Décrémenter les likes dans la table posts
            db.execSQL("UPDATE $TABLE_POSTS SET $COL_POST_LIKES = $COL_POST_LIKES - 1 WHERE $COL_POST_ID = ?", arrayOf(postId))
            return false // Unliked
        } else {
            val values = ContentValues().apply {
                put(COL_LIKE_USER, userName)
                put(COL_LIKE_POST_ID, postId)
            }
            db.insert(TABLE_LIKES, null, values)
            // Incrémenter les likes dans la table posts
            db.execSQL("UPDATE $TABLE_POSTS SET $COL_POST_LIKES = $COL_POST_LIKES + 1 WHERE $COL_POST_ID = ?", arrayOf(postId))
            return true // Liked
        }
    }

    fun estLikePar(userName: String, postId: String): Boolean {
        val cursor = readableDatabase.query(TABLE_LIKES, null, "$COL_LIKE_USER = ? AND $COL_LIKE_POST_ID = ?", arrayOf(userName, postId), null, null, null)
        val liked = cursor.count > 0
        cursor.close()
        return liked
    }

    fun recupererPostsLikesPar(userName: String): List<PhotoPost> {
        val posts = mutableListOf<PhotoPost>()
        val query = """
            SELECT p.* FROM $TABLE_POSTS p
            INNER JOIN $TABLE_LIKES l ON p.$COL_POST_ID = l.$COL_LIKE_POST_ID
            WHERE l.$COL_LIKE_USER = ?
            ORDER BY p.$COL_POST_DATE DESC
        """.trimIndent()
        
        val cursor = readableDatabase.rawQuery(query, arrayOf(userName))
        if (cursor.moveToFirst()) {
            do {
                posts.add(cursorToPost(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return posts
    }

    // --- COMMENTS ---
    fun insererCommentaire(postId: String, userName: String, text: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CMT_POST_ID, postId)
            put(COL_CMT_USER, userName)
            put(COL_CMT_TEXT, text)
            put(COL_CMT_DATE, System.currentTimeMillis())
        }
        val result = db.insert(TABLE_COMMENTS, null, values)
        
        // Incrémenter le compteur de commentaires dans la table posts
        db.execSQL("UPDATE $TABLE_POSTS SET $COL_POST_COMMENTS = $COL_POST_COMMENTS + 1 WHERE $COL_POST_ID = ?", arrayOf(postId))
        
        return result
    }

    fun recupererCommentaires(postId: String): List<Commentaire> {
        val list = mutableListOf<Commentaire>()
        val cursor = readableDatabase.query(
            TABLE_COMMENTS, null, "$COL_CMT_POST_ID = ?", arrayOf(postId), null, null, "$COL_CMT_DATE ASC"
        )
        if (cursor.moveToFirst()) {
            do {
                val cmt = Commentaire(
                    userName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CMT_USER)),
                    text = cursor.getString(cursor.getColumnIndexOrThrow(COL_CMT_TEXT)),
                    dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CMT_DATE))
                )
                list.add(cmt)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun supprimerPost(postId: String) {
        val db = writableDatabase
        db.delete(TABLE_POSTS, "$COL_POST_ID = ?", arrayOf(postId))
        db.delete(TABLE_LIKES, "$COL_LIKE_POST_ID = ?", arrayOf(postId))
        db.delete(TABLE_COMMENTS, "$COL_CMT_POST_ID = ?", arrayOf(postId))
    }

    // --- EMAIL ---
    fun getEmailByName(name: String): String? {
        val cursor = readableDatabase.query(
            TABLE_USERS, arrayOf(COL_EMAIL), "$COL_NAME = ?", arrayOf(name), null, null, null
        )
        val email = if (cursor.moveToFirst()) cursor.getString(0) else null
        cursor.close()
        return email
    }

    // --- GROUPS ---
    fun creerGroupe(name: String, desc: String, creator: String): Long {
        val values = ContentValues().apply {
            put(COL_GRP_NAME, name)
            put(COL_GRP_DESC, desc)
            put(COL_GRP_CREATOR, creator)
            put(COL_GRP_DATE, System.currentTimeMillis())
        }
        val groupId = writableDatabase.insert(TABLE_GROUPS, null, values)
        // Le créateur est automatiquement membre
        if (groupId != -1L) ajouterMembreGroupe(groupId.toInt(), creator)
        return groupId
    }

    fun recupererGroupes(): List<Triple<Int, String, String>> {
        val list = mutableListOf<Triple<Int, String, String>>()
        val cursor = readableDatabase.query(TABLE_GROUPS, null, null, null, null, null, "$COL_GRP_DATE DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(Triple(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_GRP_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_GRP_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_GRP_CREATOR))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun ajouterMembreGroupe(groupId: Int, userName: String) {
        val values = ContentValues().apply {
            put(COL_GM_GROUP, groupId)
            put(COL_GM_USER, userName)
        }
        writableDatabase.insertWithOnConflict(TABLE_GROUP_MEMBERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // --- FOLLOWS ---
    fun suivre(follower: String, followed: String) {
        val values = ContentValues().apply {
            put(COL_FOLLOW_FOLLOWER, follower)
            put(COL_FOLLOW_FOLLOWED, followed)
        }
        writableDatabase.insertWithOnConflict(TABLE_FOLLOWS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun seDesabonner(follower: String, followed: String) {
        writableDatabase.delete(TABLE_FOLLOWS, "$COL_FOLLOW_FOLLOWER = ? AND $COL_FOLLOW_FOLLOWED = ?", arrayOf(follower, followed))
    }

    fun estAbonne(follower: String, followed: String): Boolean {
        val cursor = readableDatabase.query(TABLE_FOLLOWS, null,
            "$COL_FOLLOW_FOLLOWER = ? AND $COL_FOLLOW_FOLLOWED = ?", arrayOf(follower, followed), null, null, null)
        val result = cursor.count > 0
        cursor.close()
        return result
    }

    fun getNbFollowers(userName: String): Int {
        val cursor = readableDatabase.query(TABLE_FOLLOWS, arrayOf(COL_FOLLOW_FOLLOWER),
            "$COL_FOLLOW_FOLLOWED = ?", arrayOf(userName), null, null, null)
        val count = cursor.count
        cursor.close()
        return count
    }

    fun getNbFollowing(userName: String): Int {
        val cursor = readableDatabase.query(TABLE_FOLLOWS, arrayOf(COL_FOLLOW_FOLLOWED),
            "$COL_FOLLOW_FOLLOWER = ?", arrayOf(userName), null, null, null)
        val count = cursor.count
        cursor.close()
        return count
    }
}

data class Commentaire(
    val userName: String,
    val text: String,
    val dateMillis: Long
)
