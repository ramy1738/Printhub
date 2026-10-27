package com.example.data

import kotlinx.coroutines.flow.Flow

interface PrintJobDao {
    fun getAllJobs(): Flow<List<PrintJobEntity>>
    suspend fun insertJob(job: PrintJobEntity): Long
    suspend fun deleteJob(job: PrintJobEntity)
    suspend fun clearAll()
}

interface PrintShopDao {
    fun getAllShops(): Flow<List<PrintShopEntity>>
    suspend fun insertShop(shop: PrintShopEntity): Long
    suspend fun updateShop(shop: PrintShopEntity)
    suspend fun deleteShop(shop: PrintShopEntity)
    suspend fun getCount(): Int
}
