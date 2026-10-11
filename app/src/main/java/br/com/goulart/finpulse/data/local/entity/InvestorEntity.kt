package br.com.goulart.finpulse.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investors")
data class InvestorEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "first_name") val firstName: String,
    val initials: String,
)
