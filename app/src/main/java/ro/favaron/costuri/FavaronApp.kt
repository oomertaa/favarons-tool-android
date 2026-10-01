package ro.favaron.costuri

import android.app.Application
import ro.favaron.costuri.data.AppDatabase
import ro.favaron.costuri.data.HistoryRepository
import ro.favaron.costuri.data.LiveRepository

/** Manual dependency container (single database, two strictly separate repositories). */
class FavaronApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.open(this) }
    val liveRepository: LiveRepository by lazy { LiveRepository(database) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(database) }
}
