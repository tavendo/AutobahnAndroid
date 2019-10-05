package xbr.network;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.crypto.SecretBox;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;


public class KeySeries {

    private final byte[] mAPIID;
    private final BigInteger mPrice;
    private final int mInterval;
    private final SecureRandom mRandom;

    private byte[] mID;
    private byte[] mKey;
    private SecretBox mBox;
    private Map<String, Map<String, Object>> mArchive;
    private ObjectMapper mCBOR;
    private Consumer<KeySeries> mOnRotateCallback;
    private Timer mTimer;
    private String mPrefix;

    private boolean mRunning = false;

    KeySeries(byte[] apiID, BigInteger price, int interval, String prefix,
              Consumer<KeySeries> onRotate) {
        mAPIID = apiID;
        mPrice = price;
        mInterval = interval;
        mCBOR = new ObjectMapper(new CBORFactory());
        mRandom = new SecureRandom();
        mOnRotateCallback = onRotate;
        mArchive = new HashMap<>();
        mTimer = new Timer();
        mPrefix = prefix;
    }

    void start() {
        mRunning = true;
        onRotate();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                onRotate();
            }
        }, mInterval * 1000, mInterval * 1000);
    }

    void stop() {
        mTimer.cancel();
        mRunning = false;
    }

    byte[] getID() {
        return mID;
    }

    byte[] getAPIID() {
        return mAPIID;
    }

    String getPrefix() {
        return mPrefix;
    }

    byte[] getPrice() {
        return Numeric.toBytesPadded(mPrice, 32);
    }

    Map<String, Object> encrypt(Object payload) throws JsonProcessingException {
        byte[] nonce = new Random().randomBytes(
                SodiumConstants.XSALSA20_POLY1305_SECRETBOX_NONCEBYTES);

        Map<String, Object> data = new HashMap<>();
        data.put("id", mID);
        data.put("serializer", "cbor");
        data.put("ciphertext", mBox.encrypt(nonce, mCBOR.writeValueAsBytes(payload)));

        return data;
    }

    byte[] encryptKey(byte[] keyID, byte[] buyerPubKey) {
        Map<String, Object> key = mArchive.get(Numeric.toHexString(keyID));
        SealedBox sendKeyBox = new SealedBox(buyerPubKey);
        return sendKeyBox.encrypt((byte[]) key.get("key"));
    }

    private void onRotate() {
        byte[] randomData = new byte[16];
        mRandom.nextBytes(randomData);
        mID = randomData;

        mKey = new Random().randomBytes(SodiumConstants.XSALSA20_POLY1305_SECRETBOX_KEYBYTES);
        mBox = new SecretBox(mKey);

        Map<String, Object> data = new HashMap<>();
        data.put("key", mKey);
        data.put("box", mBox);
        mArchive.put(Numeric.toHexString(mID), data);

        mOnRotateCallback.accept(this);
    }
}
