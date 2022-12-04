package com.example.umd_gluten_free.extra

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.example.umd_gluten_free.data.DataOrException
import com.example.umd_gluten_free.data.Meal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealsRepository @Inject constructor(
        private val queryProductsByName: Query
) {
    suspend fun getProductsFromFirestore(): DataOrException<List<Meal>, Exception> {
        val dataOrException = DataOrException<List<Meal>, Exception>()
        try {
            dataOrException.data = queryProductsByName.get().await().map { document ->
                    document.toObject(Meal::class.java)
            }
        } catch (e: FirebaseFirestoreException) {
            dataOrException.e = e
        }
        return dataOrException
    }
}
