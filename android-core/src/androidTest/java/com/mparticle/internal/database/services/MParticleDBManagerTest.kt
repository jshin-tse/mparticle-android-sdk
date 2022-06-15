package com.mparticle.internal.database.services

import com.mparticle.testutils.AndroidUtils.Mutable
class MParticleDBManagerTest : BaseCleanInstallEachTest() {
    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testRemoveUserAttributes() {
        val manager = MParticleDBManager(context)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = java.util.HashMap<String, List<String>>()
        val attributeList: MutableList<*> = java.util.ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        manager.removeUserAttribute(removal, null)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testUserUserAttributeLists() {
        val manager = MParticleDBManager(context)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = java.util.HashMap<String, List<String>>()
        var attributeList: MutableList<*> = java.util.ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        attributeList.add("bar-2")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar-2")
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
    }

    @org.junit.Test
    @Throws(InterruptedException::class)
    fun testGetUserAttributesAsync() {
        startMParticle()
        val dbAccessThread: Mutable<Thread> = Mutable<Thread>(null)
        val manager: MParticleDBManager = object : MParticleDBManager() {
            override fun getUserAttributeSingles(mpId: Long): java.util.TreeMap<String, String> {
                dbAccessThread.value = Thread.currentThread()
                return null
            }

            override fun getUserAttributeLists(mpId: Long): java.util.TreeMap<String, List<String>> {
                return null
            }
        }
        val latch: Mutable<MPLatch> = Mutable<MPLatch>(FailureLatch())
        val callbackThread: Mutable<Thread> = Mutable<Thread>(null)

        //when not on the main thread, it should callback on the current thread, and access the DB on the same thread
        org.junit.Assert.assertNotEquals("main", Thread.currentThread().name)
        manager.getUserAttributes({ userAttributes, userAttributeLists, mpid ->
            callbackThread.value = Thread.currentThread()
            latch.value.countDown()
        }, 1)
        org.junit.Assert.assertNotNull(callbackThread.value)
        assertEquals(Thread.currentThread().name, callbackThread.value.getName())
        assertEquals(Thread.currentThread().name, dbAccessThread.value.getName())
        callbackThread.value = null
        dbAccessThread.value = null
        latch.value = FailureLatch()

        //when run from the main thread, it should be called back on the main thread, but NOT access the DB on the same thread
        android.os.Handler(Looper.getMainLooper()).post(Runnable {
            manager.getUserAttributes({ userAttributes, userAttributeLists, mpid ->
                callbackThread.value = Thread.currentThread()
                latch.value.countDown()
            }, 1)
        })
        latch.value.await()
        org.junit.Assert.assertNotNull(callbackThread.value)
        assertEquals("main", callbackThread.value.getName())
        assertNotEquals("main", dbAccessThread.value.getName())
        //it's ok if this value changes in the future, if you know what you're doing. previously
        //this was being run on an AsyncTask, but it may have been leading to db locks, "messages"
        //thread is know to not be an issue w/db access
        assertEquals("mParticleMessageHandler", dbAccessThread.value.getName())
    }
}