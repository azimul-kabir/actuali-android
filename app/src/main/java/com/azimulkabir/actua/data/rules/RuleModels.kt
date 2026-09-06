package com.azimulkabir.actua.data.rules

import org.json.JSONArray
import org.json.JSONObject

sealed interface RuleValue {
    data class Text(val value: String) : RuleValue
    data class Number(val value: Double) : RuleValue
    data class Flag(val value: Boolean) : RuleValue
    data class ListValue(val value: List<RuleValue>) : RuleValue
    data class ObjectValue(val value: Map<String, RuleValue>) : RuleValue
    data object Null : RuleValue

    val text: String? get() = (this as? Text)?.value
    val number: Double? get() = (this as? Number)?.value
    val flag: Boolean? get() = (this as? Flag)?.value
    val list: List<RuleValue>? get() = (this as? ListValue)?.value

    companion object {
        fun fromJson(value: Any?): RuleValue = when (value) {
            null, JSONObject.NULL -> Null
            is String -> Text(value)
            is Boolean -> Flag(value)
            is kotlin.Number -> Number(value.toDouble())
            is JSONArray -> ListValue((0 until value.length()).map { fromJson(value.opt(it)) })
            is JSONObject -> ObjectValue(value.keys().asSequence().associateWith { fromJson(value.opt(it)) })
            else -> Null
        }
    }
}

data class Rule(
    val id: String,
    val stage: Stage,
    val conditionsOp: ConditionsOp,
    val conditions: List<Condition>,
    val actions: List<Action>,
) {
    enum class Stage { PRE, DEFAULT, POST }
    enum class ConditionsOp { AND, OR }
    data class Condition(val op: String, val field: String, val value: RuleValue, val options: Map<String, RuleValue> = emptyMap())
    data class Action(val op: String, val field: String?, val value: RuleValue, val options: Map<String, RuleValue> = emptyMap())

    companion object {
        fun parse(id: String, stage: String?, conditionsOp: String?, conditionsJson: String?, actionsJson: String?): Rule = Rule(
            id = id,
            stage = when (stage) { "pre" -> Stage.PRE; "post" -> Stage.POST; else -> Stage.DEFAULT },
            conditionsOp = if (conditionsOp.equals("or", true)) ConditionsOp.OR else ConditionsOp.AND,
            conditions = parseArray(conditionsJson).map { item ->
                val op = item.optString("op").takeIf(String::isNotBlank) ?: error("Invalid rule condition")
                val field = item.optString("field").takeIf(String::isNotBlank) ?: error("Invalid rule condition")
                Condition(op, RuleSchema.publicField(field), RuleValue.fromJson(item.opt("value")), options(item.optJSONObject("options")))
            },
            actions = parseArray(actionsJson).map { item ->
                val op = item.optString("op").takeIf(String::isNotBlank) ?: error("Invalid rule action")
                Action(op, item.optString("field").takeIf(String::isNotBlank)?.let(RuleSchema::publicField),
                    RuleValue.fromJson(item.opt("value")), options(item.optJSONObject("options")))
            },
        )

        private fun parseArray(json: String?): List<JSONObject> {
            if (json == null) return emptyList()
            val array = JSONArray(json)
            return (0 until array.length()).map { array.getJSONObject(it) }
        }
        private fun options(json: JSONObject?): Map<String, RuleValue> = json?.keys()?.asSequence()
            ?.associateWith { RuleValue.fromJson(json.opt(it)) }.orEmpty()
    }
}

enum class RuleFieldType { ID, STRING, NUMBER, DATE, BOOLEAN }

object RuleSchema {
    private val fieldTypes = mapOf(
        "imported_payee" to RuleFieldType.STRING, "payee" to RuleFieldType.ID,
        "payee_name" to RuleFieldType.STRING, "date" to RuleFieldType.DATE,
        "notes" to RuleFieldType.STRING, "amount" to RuleFieldType.NUMBER,
        "category" to RuleFieldType.ID, "category_group" to RuleFieldType.ID,
        "account" to RuleFieldType.ID, "cleared" to RuleFieldType.BOOLEAN,
        "reconciled" to RuleFieldType.BOOLEAN, "transfer" to RuleFieldType.BOOLEAN,
        "parent" to RuleFieldType.BOOLEAN,
    )
    private val internalToPublic = mapOf(
        "isParent" to "is_parent", "isChild" to "is_child", "acct" to "account",
        "financial_id" to "imported_id", "imported_description" to "imported_payee",
        "transferred_id" to "transfer_id", "description" to "payee",
    )
    private val publicToInternal = internalToPublic.entries.associate { (key, value) -> value to key }
    fun type(field: String) = fieldTypes[field]
    fun publicField(field: String) = internalToPublic[field] ?: field
    fun internalField(field: String) = publicToInternal[field] ?: field
}
