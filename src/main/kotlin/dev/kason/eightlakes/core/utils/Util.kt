package dev.kason.eightlakes.core.utils

import arrow.core.Either
import dev.kord.common.Color
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

fun Int.toOrdinal(): String = this.toString() + when (this % 10) {
    1 -> "st"
    2 -> "nd"
    3 -> "rd"
    else -> "th"
}

fun createSqlOp(block: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> =
    SqlExpressionBuilder.block()

/** Marks the output as for internal use only; ie, the output should never reach the end user. */
@Retention(AnnotationRetention.SOURCE)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class InternalOutput

fun <L, R> Either<L, R>.leftValue(): L? = if (this is Either.Left) value else null
fun <L, R> Either<L, R>.rightValue(): R? = if (this is Either.Right) value else null

typealias Possible<T> = Either<String, T>

fun kColor(hexCode: String): Color? = runCatching {
    Color(Integer.decode(hexCode))
}.getOrNull()