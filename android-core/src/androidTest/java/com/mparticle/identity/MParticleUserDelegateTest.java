package com.mparticle.identity;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.ConfigManager;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class MParticleUserDelegateTest extends BaseCleanStartedEachTest {
    MParticleUserDelegate mUserDelegate;

    @Before
    public void before() throws Exception {
        mUserDelegate = MParticle.getInstance().Identity().mUserDelegate;
    }

    @Test
    public void testSetGetUserIdentities() throws Exception {
        Map<Long, Map<MParticle.IdentityType, String>> attributes = new HashMap<Long, Map<MParticle.IdentityType, String>>();
        for (int i = 0; i < 5; i++) {
            Long mpid = ran.nextLong();
            Map<MParticle.IdentityType, String> pairs = new HashMap<MParticle.IdentityType, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < 3; j++) {
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandomUtils.randomInt(0, MParticle.IdentityType.values().length));
                String value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 25));
                assertTrue(mUserDelegate.setUserIdentity(value, identityType, mpid));
                pairs.put(identityType, value);
            }
        }

        com.mparticle.internal.AccessUtils.awaitMessageHandler();

        Map<Long, Map<MParticle.IdentityType, String>> storedUsersTemp = new HashMap<Long, Map<MParticle.IdentityType, String>>();

        for (Entry<Long, Map<MParticle.IdentityType, String>> user : attributes.entrySet()) {
            Map<MParticle.IdentityType, String> storedUserAttributes = mUserDelegate.getUserIdentities(user.getKey());
            storedUsersTemp.put(user.getKey(), storedUserAttributes);
            for (Entry<MParticle.IdentityType, String> pairs : user.getValue().entrySet()) {
                Object currentAttribute = storedUserAttributes.get(pairs.getKey());
                if (currentAttribute == null) {
                    Log.e("Stuff", "more stuff");
                }
                assertEquals(storedUserAttributes.get(pairs.getKey()), pairs.getValue());
            }
        }
    }

    @Test
    public void testInsertRetrieveDeleteUserAttributes() throws Exception {
        // create and store
        Map<Long, Map<String, String>> attributes = new HashMap<Long, Map<String, String>>();
        for (int i = 0; i < 5; i++) {
            Long mpid = ran.nextLong();
            Map<String, String> pairs = new HashMap<String, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < 3; j++) {
                String key = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55)).toUpperCase();
                String value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55));
                assertTrue(mUserDelegate.setUserAttribute(key, value, mpid, false));
                pairs.put(key, value);
            }
        }

        AccessUtils.awaitMessageHandler();

        // retrieve and compare
        for (Entry<Long, Map<String, String>> user : attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(user.getKey());
            for (Entry<String, String> pairs : user.getValue().entrySet()) {
                assertEquals(storedUserAttributes.get(pairs.getKey()).toString(), pairs.getValue());
            }
        }

        // delete
        for (Entry<Long, Map<String, String>> userAttributes : attributes.entrySet()) {
            for (Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertTrue(mUserDelegate.removeUserAttribute(attribute.getKey(), userAttributes.getKey()));
            }
        }

        AccessUtils.awaitMessageHandler();

        for (Entry<Long, Map<String, String>> userAttributes : attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(userAttributes.getKey());
            for (Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertNull(storedUserAttributes.get(attribute.getKey()));
            }
        }
    }

    @Test
    public void testSetConsentState() throws Exception {
        Long mpid = ran.nextLong();
        Long mpid2 = ran.nextLong();
        ConsentState state = mUserDelegate.getConsentState(mpid);
        assertNotNull(state);
        assertNotNull(state.getGDPRConsentState());
        assertEquals(0, state.getGDPRConsentState().size());

        ConsentState.Builder builder = ConsentState.builder();
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid);
        builder.addGDPRConsentState("foo2", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid2);

        assertEquals(1, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid).getGDPRConsentState().containsKey("foo"));

        assertEquals(2, mUserDelegate.getConsentState(mpid2).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid2).getGDPRConsentState().containsKey("foo"));
        assertTrue(mUserDelegate.getConsentState(mpid2).getGDPRConsentState().containsKey("foo2"));
    }

    @Test
    public void testRemoveConsentState() throws Exception {
        Long mpid = ran.nextLong();
        ConsentState state = mUserDelegate.getConsentState(mpid);
        assertNotNull(state);
        assertNotNull(state.getGDPRConsentState());
        assertEquals(0, state.getGDPRConsentState().size());

        ConsentState.Builder builder = ConsentState.builder();
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid);

        assertEquals(1, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid).getGDPRConsentState().containsKey("foo"));
        mUserDelegate.setConsentState(null, mpid);
        assertEquals(0, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());
    }

    @Test
    public void testGetUserAttributesListener() throws InterruptedException {
        Map<String, String> attributeSingles = mRandomUtils.getRandomAttributes(5, false);

        Map<String, List<String>> attributeLists = new HashMap<String, List<String>>();
        for (Entry<String, String> entry: attributeSingles.entrySet()) {
            attributeLists.put(entry.getKey() + entry.getValue(), Collections.singletonList(entry.getValue() + entry.getKey()));
        }

        for (Entry<String, List<String>> entry: attributeLists.entrySet()) {
            mUserDelegate.setUserAttributeList(entry.getKey(), entry.getValue(), mStartingMpid);
        }
        for (Entry<String, String> entry: attributeSingles.entrySet()) {
            mUserDelegate.setUserAttribute(entry.getKey(), entry.getValue(), mStartingMpid);
        }
        AccessUtils.awaitMessageHandler();

        final Mutable<Map<String, String>> userAttributesResults = new Mutable<Map<String, String>>(null);
        final Mutable<Map<String, List<String>>> userAttributeListResults = new Mutable<Map<String, List<String>>>(null);

        //fetch on the current (non-main) thread
        mUserDelegate.getUserAttributes(new UserAttributeListener() {
            @Override
            public void onUserAttributesReceived(@Nullable Map<String, String> userAttributes, @Nullable Map<String, List<String>> userAttributeLists, @Nullable Long mpid) {
                userAttributesResults.value = userAttributes;
                userAttributeListResults.value = userAttributeLists;
            }
        }, mStartingMpid);

        assertMapEquals(attributeSingles, userAttributesResults.value);
        assertMapEquals(attributeLists, userAttributeListResults.value);

        userAttributesResults.value = null;
        userAttributeListResults.value = null;

        //fetch on the main thread (seperate code path)
        final CountDownLatch latch = new MPLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mUserDelegate.getUserAttributes(new UserAttributeListener() {
                    @Override
                    public void onUserAttributesReceived(@Nullable Map<String, String> userAttributes, @Nullable Map<String, List<String>> userAttributeLists, @Nullable Long mpid) {
                        userAttributesResults.value = userAttributes;
                        userAttributeListResults.value = userAttributeLists;
                        latch.countDown();
                    }
                }, mStartingMpid);
            }
        });
        latch.await();

        assertMapEquals(attributeSingles, userAttributesResults.value);
        assertMapEquals(attributeLists, userAttributeListResults.value);
    }

    private void assertMapEquals(Map map1, Map map2) {
        assertEquals(map1.toString() + "\n\nvs" + map2.toString(), map1.size(), map2.size());
        for (Object obj: map1.entrySet()) {
            Entry entry = (Entry)obj;
            assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
    }
}
