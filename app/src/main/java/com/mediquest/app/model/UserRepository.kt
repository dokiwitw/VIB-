package com.mediquest.app.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun saveUser(user: User) {
        usersCollection.document(user.id).set(
            mapOf(
                "id" to user.id,
                "nome" to user.nome,
                "xp" to user.xp,
                "nivel" to user.nivel,
                "ultimaAtividade" to System.currentTimeMillis()
            )
        ).await()
    }

    fun getGlobalRanking(): Flow<List<RankingEntry>> = callbackFlow {
        val subscription = usersCollection
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val ranking = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val user = User(
                            id = doc.getString("id") ?: doc.id,
                            nome = doc.getString("nome") ?: "Explorador",
                            xp = doc.getLong("xp") ?: 0L,
                            nivel = doc.getLong("nivel")?.toInt() ?: 1
                        )
                        RankingEntry(
                            user = user,
                            nome = user.nome,
                            reportes = (user.xp / 10).toInt()
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList<RankingEntry>()
                
                trySend(ranking)
            }
        awaitClose { subscription.remove() }
    }
}
