package com.mparticle.internal.database.services;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.TypedUserAttributeListener;
import com.mparticle.UserAttributeListener;
import com.mparticle.identity.UserAttributeListenerWrapper;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.testutils.MPLatch;

import junit.framework.Assert;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class MParticleDBManagerTest extends BaseCleanInstallEachTest{

    @Test
    public void testRemoveUserAttributes() throws Exception {
        MParticleDBManager manager = new MParticleDBManager(mContext);
        MParticleDBManager.UserAttributeRemoval removal = new MParticleDBManager.UserAttributeRemoval();
        removal.key = "foo";
        removal.mpId = 10L;
        manager.removeUserAttribute(removal, null);
        Map<String, Object> attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
        MParticleDBManager.UserAttributeResponse newAttributes = new MParticleDBManager.UserAttributeResponse();
        newAttributes.mpId = 10L;
        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        manager.removeUserAttribute(removal, null);
        attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
    }

    @Test
    public void testUserUserAttributeLists() throws Exception {
        MParticleDBManager manager = new MParticleDBManager(mContext);
        MParticleDBManager.UserAttributeRemoval removal = new MParticleDBManager.UserAttributeRemoval();
        removal.key = "foo";
        removal.mpId = 10L;
        manager.removeUserAttribute(removal, null);
        Map<String, Object> attributes = manager.getUserAttributes(10);
        Assert.assertNull(attributes.get("foo"));
        MParticleDBManager.UserAttributeResponse newAttributes = new MParticleDBManager.UserAttributeResponse();
        newAttributes.mpId = 10L;
        newAttributes.attributeLists = new HashMap<String, List<String>>();
        List attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        attributeList.add("baz");
        attributeList.add("bar-2");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar-2");
        attributeList.add("bar");
        attributeList.add("baz");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));

        attributeList = new ArrayList<String>();
        attributeList.add("bar");
        newAttributes.attributeLists.clear();
        newAttributes.attributeLists.put("foo", attributeList);
        manager.setUserAttribute(newAttributes);
        attributes = manager.getUserAttributes(10);
        Assert.assertNotNull(attributes.get("foo"));
        Assert.assertEquals(attributeList, attributes.get("foo"));
    }

    @Test
    public void testGetUserAttributesAsync() throws InterruptedException {
        startMParticle();

        final Mutable<Thread> dbAccessThread = new Mutable<Thread>(null);
        final MParticleDBManager manager = new MParticleDBManager(){
            @Override
            public Map<String, Object> getUserAttributeSingles(long mpId) {
                dbAccessThread.value = Thread.currentThread();
                return null;
            }

            @Override
            public TreeMap<String, List<String>> getUserAttributeLists(long mpId) {
                return null;
            }
        };

        final Mutable<MPLatch> latch = new Mutable<MPLatch>(new MPLatch(1));
        final Mutable<Thread> callbackThread = new Mutable<Thread>(null);

        //when not on the main thread, it should callback on the current thread, and access the DB on the same thread
        assertNotEquals("main", Thread.currentThread().getName());

        TypedUserAttributeListener listener = new TypedUserAttributeListener() {
            @Override
            public void onUserAttributesReceived(@NonNull Map<String, ?> userAttributes, @NonNull Map<String, ? extends List<String>> userAttributeLists, long mpid) {
                callbackThread.value = Thread.currentThread();
                latch.value.countDown();
            }
        };
        manager.getUserAttributes(new UserAttributeListenerWrapper(listener), 1);

        assertNotNull(callbackThread.value);
        assertEquals(Thread.currentThread().getName(), callbackThread.value.getName());
        assertEquals(Thread.currentThread().getName(), dbAccessThread.value.getName());
        callbackThread.value = null;
        dbAccessThread.value = null;

        latch.value = new MPLatch(1);

        //when run from the main thread, it should be called back on the main thread, but NOT access the DB on the same thread
        TypedUserAttributeListener listener1 = new TypedUserAttributeListener() {
            @Override
            public void onUserAttributesReceived(@NonNull Map<String, ?> userAttributes, @NonNull Map<String, ? extends List<String>> userAttributeLists, long mpid) {
                callbackThread.value = Thread.currentThread();
                latch.value.countDown();
            }
        };
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                manager.getUserAttributes(new UserAttributeListenerWrapper(listener), 1);
            }
        });
        latch.value.await();

        assertNotNull(callbackThread.value);
        assertEquals("main", callbackThread.value.getName());
        assertNotEquals("main", dbAccessThread.value.getName());
        //it's ok if this value changes in the future, if you know what you're doing. previously
        //this was being run on an AsyncTask, but it may have been leading to db locks, "messages"
        //thread is know to not be an issue w/db access
        assertEquals("mParticleMessageHandler", dbAccessThread.value.getName());
    }
}