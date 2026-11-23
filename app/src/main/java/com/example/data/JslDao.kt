package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JslDao {
    @Query("SELECT * FROM workers ORDER BY name COLLATE NOCASE ASC")
    fun observeWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllWorkersOnce(): List<WorkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("SELECT * FROM attendance WHERE dateKey LIKE :monthPrefix || '%'")
    fun observeAttendanceForMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceOnce(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendances(items: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE workerId = :workerId AND dateKey = :dateKey")
    suspend fun deleteAttendance(workerId: Long, dateKey: String)

    @Query("SELECT * FROM advances WHERE dateKey LIKE :monthPrefix || '%' ORDER BY dateKey DESC, id DESC")
    fun observeAdvancesForMonth(monthPrefix: String): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances ORDER BY dateKey DESC, id DESC")
    suspend fun getAllAdvancesOnce(): List<AdvanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvance(advance: AdvanceEntity): Long

    @Delete
    suspend fun deleteAdvance(advance: AdvanceEntity)

    @Query("DELETE FROM advances WHERE id = :advanceId")
    suspend fun deleteAdvanceById(advanceId: Long)

    @Query("DELETE FROM attendance")
    suspend fun clearAllAttendance()

    @Query("DELETE FROM advances")
    suspend fun clearAllAdvances()

    @Query("DELETE FROM workers")
    suspend fun clearAllWorkers()

    @Transaction
    suspend fun replaceAllData(
        workers: List<WorkerEntity>,
        attendanceBuilder: (Map<Long, Long>) -> List<AttendanceEntity>,
        advancesBuilder: (Map<Long, Long>) -> List<AdvanceEntity>
    ) {
        clearAllAttendance()
        clearAllAdvances()
        clearAllWorkers()

        val idMap = mutableMapOf<Long, Long>()
        for (worker in workers) {
            val oldId = worker.id
            val insertedId = insertWorker(worker.copy(id = 0))
            idMap[oldId] = insertedId
        }

        val mappedAttendance = attendanceBuilder(idMap)
        if (mappedAttendance.isNotEmpty()) {
            upsertAttendances(mappedAttendance)
        }

        val mappedAdvances = advancesBuilder(idMap)
        for (adv in mappedAdvances) {
            insertAdvance(adv.copy(id = 0))
        }
    }
}
