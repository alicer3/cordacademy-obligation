package io.cordacademy.webserver.areas

import com.sun.xml.internal.ws.developer.MemberSubmissionAddressing
import io.cordacity.koto.extensions.EMPTY
import io.cordacity.koto.extensions.toUpperSnakeCase
import io.cordacity.koto.validation.MemberAssertionContext
import io.cordacity.koto.validation.MemberAssertionType
import io.cordacity.koto.validation.ValidationCondition
import net.corda.core.identity.CordaX500Name
import java.util.*

private fun CordaX500Name.Companion.canParse(value: String?): Boolean {
    return try {
        parse(value ?: String.EMPTY)
        true
    } catch (ex: Exception) {
        false
    }
}

fun MemberAssertionContext<out String?>.mustBeValidCordaX500Name(
    message: String = "must be a valid Corda X.500 name.",
    type: MemberAssertionType = MemberAssertionType.RELATIVE
) {
    val id = object : Any() {}.javaClass.enclosingMethod.name
    val condition = object : ValidationCondition() {
        override val id: String = id.toUpperSnakeCase()
        override fun isValid() = CordaX500Name.canParse(subject)
    }

    validate(condition, message, type)
}

fun MemberAssertionContext<out String?>.mustBeValidCurrency(
    message: String = "must be a valid currency.",
    type: MemberAssertionType = MemberAssertionType.RELATIVE
) {
    val id = object : Any() {}.javaClass.enclosingMethod.name
    val condition = object : ValidationCondition() {
        override val id: String = id.toUpperSnakeCase()
        override fun isValid(): Boolean {
            return try {
                Currency.getInstance(subject ?: String.EMPTY)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    validate(condition, message, type)
}