package app.lineo.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.lineo.data.db.entity.FormulaEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FormulaDao {

    @Query("SELECT * FROM formulas ORDER BY name ASC")
    fun formulasStream(): Flow<List<FormulaEntity>>

    @Upsert
    suspend fun upsert(formula: FormulaEntity)

    @Query("DELETE FROM formulas WHERE id = :id")
    suspend fun delete(id: Long)
}
