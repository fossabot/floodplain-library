package io.floodplain.kotlindsl.message

import io.floodplain.immutable.api.ImmutableMessage
import io.floodplain.immutable.factory.ImmutableFactory
import java.util.stream.Collectors
import kotlin.streams.toList

private val logger = mu.KotlinLogging.logger {}

data class IMessage(private val content: MutableMap<String, Any>) {

    //    val content = mapOf<String,Any>()
    operator fun get(path: String): Any? {
        val (msg, name) = parsePath(path.split("/"))
        return msg.content.get(name)
    }

    fun string(path: String): String {
        return optionalString(path)
                ?: throw NullPointerException("Can't obtain string from path: $path as it is absent")
    }

    fun integer(path: String): Int {
        return optionalInteger(path) ?: throw NullPointerException("Can't obtain int from path: $path as it is absent")
    }

    fun optionalInteger(path: String): Int? {
        val raw = get(path) ?: return null
        if (raw !is Int) {
            throw ClassCastException("Path element $path should be an integer but it is a ${raw::class}")
        }
        return raw
    }

    fun optionalString(path: String): String? {
        val raw = get(path) ?: return null
        if (raw !is String) {
            throw ClassCastException("Path element $path should be an string but it is a ${raw::class}")
        }
        return raw
    }

    private fun parsePath(path: List<String>): Pair<IMessage, String> {
        if (path.size > 1) {
            val subelement: Any? = content[path[0]]
            subelement ?: throw NullPointerException("Can't parse path list: $path, element ${path[0]} is missing")
            if (subelement !is IMessage) {
                throw ClassCastException("Path element ${path[0]} should be a message but it is a ${subelement::class}")
            }
            return subelement.parsePath(path.subList(1, path.size))
        }
        return Pair(this, path[0])

    }

    fun clear(path: String) {
        val (msg, name) = parsePath(path.split("/"))
        msg.content.remove(name)
    }

    fun isEmpty(): Boolean {
        return content.isEmpty()
    }

    operator fun set(path: String, value: Any?): IMessage {
        val (msg, name) = parsePath(path.split("/"))
        if (value == null) {
            msg.content.remove(name)
        } else {
            if(value is IMessage) {
                if(!value.isEmpty()) {
                    msg.content[name] = value
                } else {
                    // empty msg
                }
            } else {
//                if (value is List<*>) {
//                    logger.info { "L: $value" }
//                }
                // regular value
                msg.content[name] = value
            }
        }
        return this
    }

    fun toImmutable(): ImmutableMessage {
        val values = mutableMapOf<String, Any>()
        val types = mutableMapOf<String, ImmutableMessage.ValueType>()
        val subMessage = mutableMapOf<String, ImmutableMessage>()
        val subMessageList = mutableMapOf<String, List<ImmutableMessage>>()
        for ((name, elt) in content) {
            when (elt) {
                is IMessage -> subMessage[name] = elt.toImmutable()
                is List<*> -> subMessageList[name] = subListToImmutable(elt as List<IMessage>)
                is ImmutableMessage -> subMessage[name] = elt
                else -> {
                    values[name] = elt; types.put(name, ImmutableFactory.resolveTypeFromValue(elt))
                }
            }
        }
        return ImmutableFactory.create(values, types, subMessage, subMessageList)
    }

    private fun subListToImmutable(items: List<IMessage>): List<ImmutableMessage> {
        return items.stream().map {
            it.toImmutable()
        }.collect(Collectors.toList())
    }

    override fun toString(): String {
        return this.content.toString()
    }
}

fun empty(): IMessage = IMessage(mutableMapOf())


fun fromImmutable(msg: ImmutableMessage): IMessage {
    val content = mutableMapOf<String, Any>()
    for ((name, value: Any?) in msg.values()) {
        if (value != null) {
            content[name] = value
        }
    }
    for ((name, value) in msg.subMessageMap()) {
        content[name] =  fromImmutable(value)
    }
    for ((name, value) in msg.subMessageListMap()) {
        content[name] = value.stream().map { e-> fromImmutable(e) }.toList()
    }
    return IMessage(content)
}