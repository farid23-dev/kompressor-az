package az.kompressor.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import az.kompressor.app.data.local.dao.FavoriteCarDao
import az.kompressor.app.data.local.entity.FavoriteCarEntity

@Database(
    entities = [FavoriteCarEntity::class],
    version = 2,
    exportSchema = false
)
abstract class KompressorDatabase : RoomDatabase() {
    abstract fun favoriteCarDao(): FavoriteCarDao
}
