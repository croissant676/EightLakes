package dev.kason.eightlakes

import com.typesafe.config.Config
import dev.kason.eightlakes.students.Students
import dev.kason.eightlakes.utils.*
import org.jetbrains.exposed.sql.*
import org.kodein.di.*

class EightLakesData(override val di: DI) : ConfigAware(di) {
    companion object : ModuleProducer {
        override suspend fun createModule(config: Config): DI.Module {
            return DI.Module(name = "data_module") {
                bindSingleton { EightLakesData(di) }
                bindSingleton { connectToDatabase(config) }
            }
        }


        private fun connectToDatabase(config: Config): Database {
            return Database.connect(
                url = config.getString("data.url"),
                user = config.getString("data.user"),
                password = config.getString("data.password"),
                databaseConfig = DatabaseConfig {
                    sqlLogger = Slf4jSqlDebugLogger
                }
            )
        }
    }

    private val tables = setOf(
        Students // add more tables here
    )
}