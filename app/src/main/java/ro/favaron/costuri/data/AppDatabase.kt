package ro.favaron.costuri.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        IngredientEntity::class,
        RecipeEntity::class,
        RecipeLineEntity::class,
        OrderEntity::class,
        OrderProductEntity::class,
        OrderIngredientLineEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liveDao(): LiveDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val NAME = "favaron.db"

        // No destructive migration fallback: a schema change without a
        // migration must fail loudly rather than wipe the pastry chef's data.
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
