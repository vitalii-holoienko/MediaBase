package com.example.filmsdataapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.filmsdataapp.R
import com.example.filmsdataapp.domain.model.ActivityData
import com.example.filmsdataapp.domain.model.Title
import com.example.filmsdataapp.domain.repository.UserRepository
import com.example.filmsdataapp.domain.usecase.DeleteTitleFromAllListsUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
) : UserRepository {

    override fun createUserAccount(nickname: String, description: String, image: Uri?, onSuccess: () -> Unit) {
        val finalImageUri =
            image ?: Uri.parse("android.resource://${context.packageName}/${R.drawable.user_icon}")
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val profileUpdates = userProfileChangeRequest {
            displayName = nickname
            photoUri = finalImageUri
        }

        uploadImageToStorage(finalImageUri) { downloadUrl ->
            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = hashMapOf(
                            "nickname" to nickname,
                            "description" to description,
                            "image" to downloadUrl
                        )

                        firestore.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                Log.d("DEBUG", "User data saved with UID: $uid")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.w("DEBUG", "Error adding user document", e)
                            }
                    } else {
                        Log.e("DEBUG", "Failed to update profile", task.exception)
                    }
                }
        }


    }

    override fun setUserRatingForTitle(title: Title, rating: Float, where: String) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore
        val titleId = title.id ?: return
        val titleName = title.primaryTitle ?: "Unknown title"
        val docRef = db.collection("users")
            .document(uid)
            .collection(where)
            .document(titleId)
        docRef.update("userRating", (rating*2).toInt())
            .addOnSuccessListener {
                val historyRef = db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document()
                val historyEntry = mapOf(
                    "message" to "$titleName was rated ${(rating*2).toInt()}.",
                    "timestamp" to FieldValue.serverTimestamp()
                )
                historyRef.set(historyEntry)
                    .addOnSuccessListener {
                        Log.d("HISTORY", "History entry added.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("HISTORY", "Failed to add history entry", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RATING", "Failed to update rating for $titleId", e)
            }

    }

    override fun getUserRatingForTitle(titleId: String, where: String, onResult: (Int?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        val docRef = db.collection("users")
            .document(uid)
            .collection(where)
            .document(titleId)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val rating = snapshot.getLong("userRating")?.toInt()
                    onResult(rating)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RATING", "Failed to load userRating", e)
                onResult(null)
            }
    }

    override fun changeUserAccount(nickname: String, description: String, image: Uri?, onSuccess: () -> Unit) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid


        if (image != null && image.scheme != "https") {
            uploadImageToStorage(image) { downloadUrl ->
                applyProfileChanges(currentUser, nickname, downloadUrl, description, uid,onSuccess )
            }
        } else {

            val existingPhotoUrl = currentUser.photoUrl?.toString()
                ?: "https://..."

            applyProfileChanges(currentUser, nickname, existingPhotoUrl, description, uid, onSuccess)
        }
    }

    override fun getUserImage(callback: (Uri) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageString = document.getString("image")
                    val imageUri = imageString?.let { Uri.parse(it) }
                    callback(imageUri!!)
                }


            }
            .addOnFailureListener { exception ->

            }
    }

    override fun getUserDescription(callback: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val description = document.getString("description") ?: "-"
                    callback(description)
                } else {
                    Log.d("TEKKEN", "No such document")
                    callback("-")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TEKKEN", "get failed with ", exception)
                callback("-")
            }
    }

    override fun getMonthlyCompletedStats(uid: String, onResult: (List<ActivityData>) -> Unit) {
        val db = firestore
        db.collection("users")
            .document(uid)
            .collection("completed")
            .get()
            .addOnSuccessListener { result ->

                val keyFormat = SimpleDateFormat("yyyy-MM", Locale.ENGLISH)

                val dateCounts = mutableMapOf<String, Int>()
                result.documents.forEach { doc ->
                    val date = doc.getTimestamp("addedAt")?.toDate() ?: return@forEach
                    val key = keyFormat.format(date)
                    dateCounts[key] = dateCounts.getOrDefault(key, 0) + 1
                }

                val completeData = mutableListOf<ActivityData>()

                val baseCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, -10)
                }

                for (i in 0 until 11) {
                    val cal = baseCal.clone() as Calendar
                    cal.add(Calendar.MONTH, i)
                    val key = keyFormat.format(cal.time)
                    val count = dateCounts.getOrDefault(key, 0)
                    completeData.add(ActivityData(key, count))
                }

                onResult(completeData)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    override fun checkIfUserHasTitleInLists(titleId: String, callback: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            callback("no")
            return
        }

        val db = firestore
        val collections = listOf("onhold", "dropped", "completed", "watching", "planned")
        var checkedCount = 0
        var found = false

        for (collection in collections) {
            db.collection("users")
                .document(uid)
                .collection(collection)
                .document(titleId)
                .get()
                .addOnSuccessListener { document ->
                    checkedCount++
                    if (document.exists() && !found) {
                        found = true
                        callback(collection)
                    } else if (checkedCount == collections.size && !found) {
                        callback("no")
                    }
                }
                .addOnFailureListener {
                    checkedCount++
                    if (checkedCount == collections.size && !found) {
                        callback("no")
                    }
                }
        }
    }

    override suspend fun deleteTitleFromAllLists(id: String) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore
        val collections = listOf("planned", "watching", "completed", "onhold", "dropped")

        for (collection in collections) {
            val docRef = db.collection("users").document(uid).collection(collection).document(id)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.delete().await()
            }
        }
    }

    override fun getUserNickname(callback: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "Default"
                    callback(nickname)
                } else {
                    Log.d("DEBUG", "No such document")
                    callback("Default")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("DEBUG", "get failed with ", exception)
                callback("Default")
            }
    }

    override fun fetchUserHistory(onResult: (List<String>) -> Unit, onError: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        db.collection("users")
            .document(uid)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val historyMessages = result.documents.mapNotNull { doc ->
                    doc.getString("message")
                }
                onResult(historyMessages)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    override fun addTitleToWatchingList(title: Title) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        val titleId = title.id ?: return
        val titleName = title.primaryTitle?: "Unknown Title"
            val watchingRef = db.collection("users")
                .document(uid)
                .collection("watching")
                .document(titleId)

            watchingRef.set(title)
                .addOnSuccessListener {

                    watchingRef.update("addedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            Log.d("WATCHING", "Title $titleId added to watching list.")


                            val historyRef = db.collection("users")
                                .document(uid)
                                .collection("history")
                                .document()

                            val historyEntry = mapOf(
                                "message" to "$titleName was added to 'Watched' list.",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            historyRef.set(historyEntry)
                                .addOnSuccessListener {
                                    Log.d("HISTORY", "History entry added.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HISTORY", "Failed to add history entry", e)
                                }

                        }
                        .addOnFailureListener { e ->
                            Log.e("WATCHING", "Failed to update addedAt field", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("WATCHING", "Failed to add title to watching list", e)
                }
    }

    override fun addTitleToPlannedList(title: Title) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        val titleId = title.id ?: return
        val titleName = title.primaryTitle?: "Unknown Title"
            val watchingRef = db.collection("users")
                .document(uid)
                .collection("planned")
                .document(titleId)
            watchingRef.set(title)
                .addOnSuccessListener {

                    watchingRef.update("addedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            val historyRef = db.collection("users")
                                .document(uid)
                                .collection("history")
                                .document()

                            val historyEntry = mapOf(
                                "message" to "$titleName was added to 'Planned' list.",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            historyRef.set(historyEntry)
                                .addOnSuccessListener {
                                    Log.d("HISTORY", "History entry added.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HISTORY", "Failed to add history entry", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("planned", "Failed to update addedAt field", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("planned", "Failed to add title to watching list", e)
                }
    }


    override fun addTitleToCompletedList(title: Title) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        val titleId = title.id ?: return
        val titleName = title.primaryTitle?: "Unknown Title"
            val watchingRef = db.collection("users")
                .document(uid)
                .collection("completed")
                .document(titleId)


            watchingRef.set(title)
                .addOnSuccessListener {

                    watchingRef.update("addedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            val historyRef = db.collection("users")
                                .document(uid)
                                .collection("history")
                                .document()

                            val historyEntry = mapOf(
                                "message" to "$titleName was added to 'Planned' list.",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            historyRef.set(historyEntry)
                                .addOnSuccessListener {
                                    Log.d("HISTORY", "History entry added.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HISTORY", "Failed to add history entry", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("planned", "Failed to update addedAt field", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("planned", "Failed to add title to watching list", e)
                }

    }

    override fun addTitleToOnHoldList(title: Title) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        val titleId = title.id ?: return
        val titleName = title.primaryTitle?: "Unknown Title"

            val watchingRef = db.collection("users")
                .document(uid)
                .collection("onhold")
                .document(titleId)

            watchingRef.set(title)
                .addOnSuccessListener {

                    watchingRef.update("addedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            val historyRef = db.collection("users")
                                .document(uid)
                                .collection("history")
                                .document()

                            val historyEntry = mapOf(
                                "message" to "$titleName was added to 'Planned' list.",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            historyRef.set(historyEntry)
                                .addOnSuccessListener {
                                    Log.d("HISTORY", "History entry added.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HISTORY", "Failed to add history entry", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("planned", "Failed to update addedAt field", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("planned", "Failed to add title to watching list", e)
                }

    }

    override fun addTitleToDroppedList(title: Title) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore
        val titleName = title.primaryTitle?: "Unknown Title"
        val titleId = title.id ?: return

            val watchingRef = db.collection("users")
                .document(uid)
                .collection("dropped")
                .document(titleId)

            watchingRef.set(title)
                .addOnSuccessListener {

                    watchingRef.update("addedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            val historyRef = db.collection("users")
                                .document(uid)
                                .collection("history")
                                .document()

                            val historyEntry = mapOf(
                                "message" to "$titleName was added to 'Planned' list.",
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            historyRef.set(historyEntry)
                                .addOnSuccessListener {
                                    Log.d("HISTORY", "History entry added.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("HISTORY", "Failed to add history entry", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("dropped", "Failed to update addedAt field", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("dropped", "Failed to add title to watching list", e)
                }

    }

    override fun getDroppedTitles(callback: (List<Title>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        db.collection("users")
            .document(uid)
            .collection("dropped")
            .get()
            .addOnSuccessListener { result ->
                val titles = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("WATCHING", "Failed to parse title from Firestore", e)
                        null
                    }
                }
                callback(titles)
            }
            .addOnFailureListener { e ->
                Log.e("WATCHING", "Failed to get watching titles", e)
                callback(emptyList())
            }
    }

    override fun getCompletedTitles(callback: (List<Title>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore
        db.collection("users")
            .document(uid)
            .collection("completed")
            .get()
            .addOnSuccessListener { result ->
                val titles = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("WATCHING", "Failed to parse title from Firestore", e)
                        null
                    }
                }
                callback(titles)
            }
            .addOnFailureListener { e ->
                Log.e("WATCHING", "Failed to get watching titles", e)
                callback(emptyList())
            }
    }

    override fun getWatchingTitles(callback: (List<Title>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        db.collection("users")
            .document(uid)
            .collection("watching")
            .get()
            .addOnSuccessListener { result ->
                val titles = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("WATCHING", "Failed to parse title from Firestore", e)
                        null
                    }
                }
                callback(titles)
            }
            .addOnFailureListener { e ->
                Log.e("WATCHING", "Failed to get watching titles", e)
                callback(emptyList())
            }
    }

    override fun getPlannedTitles(callback: (List<Title>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val db = firestore

        db.collection("users")
            .document(uid)
            .collection("planned")
            .get()
            .addOnSuccessListener { result ->
                val titles = result.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Title::class.java)
                    } catch (e: Exception) {
                        Log.e("WATCHING", "Failed to parse title from Firestore", e)
                        null
                    }
                }
                callback(titles)
            }
            .addOnFailureListener { e ->
                Log.e("WATCHING", "Failed to get watching titles", e)
                callback(emptyList())
            }
    }

    private fun applyProfileChanges(
        currentUser: FirebaseUser,
        nickname: String,
        imageUrl: String,
        description: String,
        uid: String,
        onSuccess: () -> Unit
    ) {
        val profileUpdates = userProfileChangeRequest {
            displayName = nickname
            photoUri = Uri.parse(imageUrl)
        }

        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = hashMapOf(
                        "nickname" to nickname,
                        "description" to description,
                        "image" to imageUrl
                    )

                    firestore.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            Log.d("TEKKEN", "User data updated")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.w("TEKKEN", "Error updating user document", e)
                        }
                } else {
                    Log.e("TEKKEN", "Failed to update profile", task.exception)
                }
            }
    }

    fun uploadImageToStorage(imageUri: Uri, onUploaded: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = storage.reference
            .child("user_images/$uid.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onUploaded(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Log.e("TEKKEN", "Failed to upload image", e)
            }
    }


}