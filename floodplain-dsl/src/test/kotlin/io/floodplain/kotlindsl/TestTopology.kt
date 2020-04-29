package io.floodplain.kotlindsl

import io.floodplain.kotlindsl.message.IMessage
import io.floodplain.kotlindsl.message.empty
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


private val logger = mu.KotlinLogging.logger {}


class TestTopology {

    @Test
    fun testSimple() {
        pipe("somegen") {
            source("@sometopic") {
                sink("@outputTopic")
            }
        }.renderAndTest {
            input("@sometopic", "key1", empty().set("name", "gorilla"))
            input("@sometopic", "key1", empty().set("name", "monkey"))
            assertEquals("gorilla", output("@outputTopic").second["name"])
            assertEquals("monkey", output("@outputTopic").second["name"])
        }
    }



    @Test
    fun testDelete() {
        pipe("somegen") {
            source("@sometopic") {
                sink("@outputtopic")
            }
        }.renderAndTest {
            input("@sometopic", "key1", empty().set("name","gorilla"))
            delete("@sometopic","key1")
            output("@outputtopic")
            val eq = deleted("@outputtopic").equals("key1");
            logger.info("equal: $eq")
            logger.info ("Topic now empty: ${isEmpty("@outputtopic")}")
        }
    }


    @Test
    fun testSimpleJoin() {
        pipe("somegen") {
            source("@left") {
                join {
                    source("@right") {}
                }
                set { left, right ->
                    left["rightsub"] = right
                    left
                }
                sink("@output")
            }
        }.renderAndTest {
            assertTrue(isEmpty("@output"))
            input("@left", "key1", empty().set("name", "left1"))
            assertTrue(isEmpty("@output"))
            input("@left", "wrongkey", empty().set("name", "nomatter"))
            assertTrue(isEmpty("@output"))
            input("@right", "key1", empty().set("subname", "monkey"))
            val (_,result) = output("@output")
            logger.info("Result: $result")
            assertEquals("monkey",result["rightsub/subname"])
            delete("@left","key1")
            assertEquals("key1",deleted("@output"))
        }
    }

    @Test
    fun testOptionalSingleJoin() {
        pipe("somegen") {
            source("@left") {
                join(optional = true) {
                    source("@right") {

                    }
                }
                set { left, right ->
                    left["rightsub"] = right
                    left
                }
                sink("@output")
            }
        }.renderAndTest {
            assertTrue(isEmpty("@output"))
            val msg = empty().set("name", "left1")
            input("@left", "key1", msg)
            assertTrue(!isEmpty("@output"))
            assertEquals(output("@output").second, msg)
            input("@left", "otherkey", empty().set("name", "nomatter"))
            assertTrue(!isEmpty("@output"))
            assertEquals(output("@output").second, empty().set("name","nomatter"))
            input("@right", "key1", empty().set("subname", "monkey"))
            val (_,result) = output("@output")
            logger.info("Result: $result")
            assertEquals("monkey",result["rightsub/subname"])
            delete("@left","key1")
            assertEquals("key1",deleted("@output"))
        }
    }

    @Test
    fun testGroup() {
        pipe("somegen") {
            source("src") {
                group {message->message["subkey"] as String }
                sink("mysink")
            }
        }.renderAndTest {
            val record1 = empty().set("subkey", "subkey1")
            val record2 = empty().set("subkey", "subkey2")
            input("src","key1", record1)
            input("src","key2", record2)
            val (k1,v1) = output("mysink")
            assertEquals("subkey1|key1",k1)
            assertEquals(record1,v1)
            val (k2,v2) = output("mysink")
            assertEquals("subkey2|key2",k2)
            assertEquals(record2,v2)

            // TODO continue
        }
    }
    @Test
    fun testSingleToManyJoinOptional() {
        pipe("somegen") {
            source("@left") {
                joinGrouped(optional = true,debug = true) {
                    source("@right") {
                        group { msg->msg["foreignkey"] as String }
                    }
                }
                set { left, right ->
                    left["rightsub"] = right["list"]
                    left
                }
                each{ left,right,key->
                        logger.info( "Message: ${left} RightMessage $right key: $key")
                }
                sink("@output")
            }
        }.renderAndTest {
            assertTrue(isEmpty("@output"))
            val leftRecord = empty().set("name", "left1")
            input("@left", "key1", leftRecord)
            assertTrue(!isEmpty("@output"))
            val record1 = empty().set("foreignkey", "key1").set("recorddata","data1")
            val record2 = empty().set("foreignkey", "key1").set("recorddata","data2")
            input("@right","otherkey1", record1)
            input("@right","otherkey2", record2)

            val (key,value) = output("@output")
            assertEquals("key1",key)
            val sublist: List<IMessage> = (value["rightsub"]?: emptyList<IMessage>()) as List<IMessage>
            assertTrue (sublist.isEmpty())

            val outputs = outputSize("@output")
            assertEquals(2,outputs,"should have 2 elements")
            output("@output") // skip one
            val (_,v3) = output("@output")
            val subList = v3.get("rightsub") as List<*>
            assertEquals(2,subList.size)
            assertEquals(record1,subList[0])
            assertEquals(record2,subList[1])
            delete("@right","otherkey1")
            val (_,v4) = output("@output")
            val subList2 = v4.get("rightsub") as List<*>
            assertEquals(1,subList2.size)
            delete("@right","otherkey2")
            val (_,v5) = output("@output")
            val subList3 = v5.get("rightsub") as List<*>
            assertEquals(0,subList3.size)
            delete("@left","key1")
            assertEquals("key1",deleted("@output"))
        }
    }
    @Test
    fun testSingleToManyJoin() {
        pipe("somegen") {
            source("@left") {
                joinGrouped(debug = true) {
                    source("@right") {
                        group { msg->msg["foreignkey"] as String }
                    }
                }
                set { left, right ->
                    left["rightsub"] = right["list"]
                    left
                }
                each{ left,right,key->
                    logger.info( "Message: ${left} RightMessage $right key: $key")
                }
                sink("@output")
            }
        }.renderAndTest {
            assertTrue(isEmpty("@output"))
            val leftRecord = empty().set("name", "left1")
            input("@left", "key1", leftRecord)
//            assertTrue(!isEmpty("@output"))

            val record1 = empty().set("foreignkey", "key1").set("recorddata","data1")
            val record2 = empty().set("foreignkey", "key1").set("recorddata","data2")
            input("@right","otherkey1", record1)
            input("@right","otherkey2", record2)

            // TODO skip the 'ghost delete' I'm not too fond of this, this one should be skippable
            deleted("@output")
            val outputs = outputSize("@output")
            assertEquals(2,outputs,"should have 2 elements")
            output("@output") // skip one
            val (_,v3) = output("@output")
            val subList = v3.get("rightsub") as List<*>
            assertEquals(2,subList.size)
            assertEquals(record1,subList[0])
            assertEquals(record2,subList[1])
            delete("@right","otherkey1")
            val (_,v4) = output("@output")
            val subList2 = v4.get("rightsub") as List<*>
            assertEquals(1,subList2.size)
            delete("@right","otherkey2")
            assertEquals("key1",deleted("@output"))
        }
    }

//    @Test
//    fun testRemoteJoin() {
//        fail("Implement")
//    }

    @Test
    fun testFilter() {
        pipe("anygen") {
            source("@source") {
                filter { key,value->
                    value["name"]=="myname"
                }
                sink("@output")
            }
        }.renderAndTest {
            input("@source","key1", empty().set("name","myname"))
            input("@source","key2", empty().set("name","notmyname"))
            input("@source","key3", empty().set("name","myname"))
            assertEquals(2,outputSize("@output"))
//            val (key,value) = output("@source")
        }
    }

}
