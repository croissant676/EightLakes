package dev.kason.eightlakes.courses

import com.typesafe.config.Config
import dev.kason.eightlakes.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.kodein.di.*

object Courses : IntIdTable("courses") {
    val courseName = varchar("course_name", 255)
    val simpleName = varchar("course_code", 255)
    val courseLevel = enumeration<CourseLevel>("course_level")
    val discordRole = snowflake("discord_role")
    val discordChannel = snowflake("discord_channel")
}

class Course(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Course>(Courses)
    object Loader : ModuleProducer {
        override suspend fun createModule(config: Config): DI.Module {
            return DI.Module("course_module") {
                bindSingleton { CourseService(di) }
                bindSingleton { ClassService(di) }
                bindSingleton { RegistrationService(di) }
                bindSingleton { TeacherService(di) }
                bindEagerSingleton { CourseController(di).addToService() }
                bindEagerSingleton { TeacherController(di).addToService() }
                bindEagerSingleton { RegistrationController(di).addToService() }
                bindEagerSingleton { ClassController(di).addToService() }
            }
        }
    }

    var courseName by Courses.courseName
    var simpleName by Courses.simpleName
    var courseLevel by Courses.courseLevel
    var discordRole by Courses.discordRole
    var discordChannel by Courses.discordChannel
}
