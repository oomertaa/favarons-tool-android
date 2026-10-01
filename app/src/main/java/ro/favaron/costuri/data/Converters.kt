package ro.favaron.costuri.data

import androidx.room.TypeConverter
import java.math.BigDecimal

/** BigDecimal is stored as its exact plain string: no rounding on save. */
class Converters {
    @TypeConverter
    fun fromBigDecimal(v: BigDecimal?): String? = v?.toPlainString()

    @TypeConverter
    fun toBigDecimal(s: String?): BigDecimal? = s?.let { BigDecimal(it) }
}
