package com.example.smartsous.data.repository

import com.example.smartsous.data.local.dao.MealPlanDao
import com.example.smartsous.data.local.entity.MealPlanEntity
import com.example.smartsous.data.local.entity.toDomain
import com.example.smartsous.data.local.entity.toEntities
import com.example.smartsous.data.remote.MealPlanDto
import com.example.smartsous.data.remote.toDomain
import com.example.smartsous.domain.model.MealPlan
import com.example.smartsous.domain.model.MealType
import com.example.smartsous.domain.repository.IMealPlanRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.time.LocalDate
import javax.inject.Inject

class MealPlanRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dao: MealPlanDao
) : IMealPlanRepository {

    private val col get() = auth.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("mealPlans")
    }

    override fun getMealPlanForWeek(startDate: LocalDate): Flow<List<MealPlan>> {
        val endDate = startDate.plusDays(6)

        return channelFlow {
            // Listen to Firestore changes (+/- 15 days around the request date to minimize reads).
            val fetchStart = startDate.minusDays(15).toString()
            val fetchEnd = endDate.plusDays(15).toString()

            val registration = col?.whereGreaterThanOrEqualTo("date", fetchStart)
                ?.whereLessThanOrEqualTo("date", fetchEnd)
                ?.addSnapshotListener { snap, err ->
                    if (err != null) return@addSnapshotListener
                    val dtos = snap?.toObjects(MealPlanDto::class.java) ?: emptyList()
                    val plans = dtos.map { it.toDomain() }
                    val entities = plans.flatMap { it.toEntities() }
                    launch(Dispatchers.IO) {
                        entities.forEach { dao.upsert(it) }
                    }
                }

            val roomJob = launch {
                dao.getForWeek(startDate.toString(), endDate.toString())
                    .map { entities ->
                        entities.groupBy { it.date }.map { (dateStr, group) ->
                            group.toDomain(LocalDate.parse(dateStr))
                        }
                    }
                    .collect { trySend(it) }
            }

            awaitClose {
                registration?.remove()
                roomJob.cancel()
            }
        }
    }

    override suspend fun addRecipeToPlan(date: LocalDate, mealType: MealType, recipeId: String) {
        val dateStr = date.toString()
        
        // 1. Optimistic Update (Room)
        val existingEntity = dao.get(dateStr, mealType.name)
        val currentIds = existingEntity?.recipeIdsJson?.let { parseJson(it) } ?: mutableListOf()
        if (!currentIds.contains(recipeId)) {
            currentIds.add(recipeId)
            dao.upsert(MealPlanEntity(dateStr, mealType.name, toJson(currentIds)))
        }

        // 2. Sync to Firestore
        val docRef = col?.document(dateStr) ?: return
        try {
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                val dto = snap.toObject(MealPlanDto::class.java) ?: MealPlanDto(dateStr)
                val currentMeals = dto.meals.toMutableMap()
                val currentList = currentMeals[mealType.name]?.toMutableList() ?: mutableListOf()
                if (!currentList.contains(recipeId)) {
                    currentList.add(recipeId)
                    currentMeals[mealType.name] = currentList
                }
                tx.set(docRef, dto.copy(meals = currentMeals))
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeRecipeFromPlan(date: LocalDate, mealType: MealType, recipeId: String) {
        val dateStr = date.toString()
        
        // 1. Optimistic Update (Room)
        val existingEntity = dao.get(dateStr, mealType.name)
        val currentIds = existingEntity?.recipeIdsJson?.let { parseJson(it) } ?: mutableListOf()
        if (currentIds.contains(recipeId)) {
            currentIds.remove(recipeId)
            dao.upsert(MealPlanEntity(dateStr, mealType.name, toJson(currentIds)))
        }

        // 2. Sync to Firestore
        val docRef = col?.document(dateStr) ?: return
        try {
            firestore.runTransaction { tx ->
                val snap = tx.get(docRef)
                val dto = snap.toObject(MealPlanDto::class.java) ?: return@runTransaction
                val currentMeals = dto.meals.toMutableMap()
                val currentList = currentMeals[mealType.name]?.toMutableList() ?: return@runTransaction
                if (currentList.contains(recipeId)) {
                    currentList.remove(recipeId)
                    currentMeals[mealType.name] = currentList
                    tx.set(docRef, dto.copy(meals = currentMeals))
                }
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseJson(json: String): MutableList<String> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) { mutableListOf() }
    }

    private fun toJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }
}
