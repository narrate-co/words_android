package space.narrate.waylan.wordnik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordnikDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(definitionEntry: DefinitionEntry)

  @Query("SELECT * FROM wordnik_definitions WHERE word = :word")
  fun getDefinitionEntry(word: String): Flow<DefinitionEntry?>

  @Query("SELECT * FROM wordnik_definitions WHERE word = :word")
  suspend fun getDefinitionEntryImmediate(word: String): DefinitionEntry?
}