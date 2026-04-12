package com.mediquest.app.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val activeCollection = db.collection("active_vibers")

    private const val PREFS_NAME = "VIB_PREFS"
    private const val KEY_USER_ID = "USER_ID"

    fun getLocalUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_ID, null)
    }

    fun saveLocalUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    suspend fun getUser(userId: String): User? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            if (doc.exists()) {
                User(
                    id = doc.getString("id") ?: doc.id,
                    nome = doc.getString("nome") ?: "Explorador",
                    xp = doc.getLong("xp") ?: 0L,
                    nivel = doc.getLong("nivel")?.toInt() ?: 1,
                    avatarId = doc.getLong("avatarId")?.toInt() ?: 1,
                    photoBase64 = doc.getString("photoBase64"),
                    isGhost = doc.getBoolean("isGhost") ?: false,
                    hasVibPass = doc.getBoolean("hasVibPass") ?: false
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUser(user: User) {
        usersCollection.document(user.id).set(
            mapOf(
                "id" to user.id,
                "nome" to user.nome,
                "xp" to user.xp,
                "nivel" to user.nivel,
                "avatarId" to user.avatarId,
                "photoBase64" to user.photoBase64,
                "isGhost" to user.isGhost,
                "hasVibPass" to user.hasVibPass,
                "ultimaAtividade" to System.currentTimeMillis()
            )
        ).await()
    }

    /**
     * Atualiza a posição do usuário para o Live Vibe.
     * Se estiver em Ghost Mode, o documento é deletado para garantir privacidade.
     */
    suspend fun updateLivePosition(user: User, lat: Double, lng: Double, avatarId: Int, photoBase64: String? = null) {
        if (user.isGhost) {
            // Se o usuário entrar em modo Ghost, removemos ele do mapa imediatamente
            activeCollection.document(user.id).delete().await()
            return
        }

        val activeUser = ActiveUser(
            id = user.id,
            nome = user.nome,
            latitude = lat,
            longitude = lng,
            avatarId = avatarId,
            photoBase64 = photoBase64,
            isGhost = false,
            lastUpdate = System.currentTimeMillis()
        )
        activeCollection.document(user.id).set(activeUser).await()
    }

    /**
     * Ouve todos os usuários que atualizaram sua posição recentemente.
     * Filtramos os últimos 5 minutos para garantir que o mapa esteja "vivo".
     */
    fun getActiveVibers(): Flow<List<ActiveUser>> = callbackFlow {
        // Ouvimos a coleção inteira e filtramos o tempo no lado do cliente 
        // para garantir que o "janela de 5 minutos" se mova com o tempo real.
        val subscription = activeCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val threshold = System.currentTimeMillis() - (5 * 60 * 1000)
            val vibers = snapshot?.documents?.mapNotNull { doc ->
                val v = doc.toObject(ActiveUser::class.java)
                if (v != null && v.lastUpdate > threshold) v else null
            } ?: emptyList()
            
            trySend(vibers)
        }
        awaitClose { subscription.remove() }
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
