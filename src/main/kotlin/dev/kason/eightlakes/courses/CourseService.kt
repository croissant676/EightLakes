package dev.kason.eightlakes.courses

import dev.kason.eightlakes.DiscordEntityService
import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kodein.di.*

class CourseService(override val di: DI) : DIAware, DiscordEntityService<Course> {
    override suspend fun get(discordId: Snowflake): Course {
        return newSuspendedTransaction {
            Course.find(Courses.discordChannel eq discordId).first()
        }
    }

    override suspend fun getOrNull(discordId: Snowflake): Course? {
        return newSuspendedTransaction {
            Course.find(Courses.discordChannel eq discordId).firstOrNull()
        }
    }


}