package keys.fantom.fantomkeys;

import android.support.annotation.NonNull;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SignaApp {

    private static final BigInteger MASK_256 = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);

//    public static void main(String[] args) {
//        String keyStr = "a44fce603f0294b1a4b5ca42556d1b581b94a5ba59ac9bf2fda39ac8c315068f";
//        Object[] data = {1, 1539807565889L};
//
//        // Create keypair from private key
//        ECKeyPair key = createKeyPair(keyStr);
//        // Sign data with given key pair
//        String signature = sign(key, data);
//
//        System.out.println(signature);
//        String expected = "0x8b93124666fd1ba51f68017bb49b900aded9db1d631e783753939c7b2d96d0dd4c3925bbf83ee367992c9f17471750ca3880fb341cb824208591479e5f1b77341b";
//
//        if (!signature.equals(expected)) {
//            throw new IllegalStateException();
//        }
//
//    }

    @NonNull
    public static ECKeyPair createKeyPair(String keyStr) {
        BigInteger priv	 = new BigInteger(keyStr, 16);
        BigInteger pub = Sign.publicKeyFromPrivate(priv);
        return new ECKeyPair(priv, pub);
    }

    @NonNull
    public static String sign(ECKeyPair key, Object[] data) {
        byte[] hash = soliditySha3(data);
        SignatureData sign = Sign.signMessage(hash, key, false);

        String r = Numeric.toHexStringNoPrefix(sign.getR());
        String s = Numeric.toHexStringNoPrefix(sign.getS());
        String v = Numeric.toHexStringNoPrefix(new BigInteger("" + sign.getV()));

        System.out.println("R: " + r);
        System.out.println("S: " + s);
        System.out.println("V: " + v);

        return "0x" + r + s + v;
    }

    public static byte[] soliditySha3(Object... data) {
        if (data.length == 1) {
            return Hash.sha3(toBytes(data[0]));
        }

        List<byte[]> arrays = new ArrayList<>();
        for (Object dataObject: data) {
            arrays.add(toBytes(dataObject));
        }

        int len = 0;
        for (byte[] array: arrays) {
            len+= array.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(len);
        for (byte[] a : arrays) {
            buffer.put(a);
        }
        byte[] array = buffer.array();
        return Hash.sha3(array);
    }

    public static byte[] toBytes(Object obj) {
        if (obj instanceof byte[]) {
            int length = ((byte[]) obj).length;
            if (length < 32) {
                return Arrays.copyOf((byte[]) obj, 32);
            }
            return (byte[]) obj;
        } else if (obj instanceof Address) {
            Uint uint = ((Address) obj).toUint160();
            return Numeric.toBytesPadded(uint.getValue(), 20);
        } else if (obj instanceof BigInteger) {
            BigInteger value = (BigInteger) obj;
            if (value.signum() < 0) {
                value = MASK_256.and(value);
            }
            return Numeric.toBytesPadded(value, 32);
        } else if (obj instanceof Uint256) {
            Uint uint = (Uint) obj;
            return Numeric.toBytesPadded(uint.getValue(), 32);
        } else if (obj instanceof Uint64) {
            Uint uint = (Uint) obj;
            return Numeric.toBytesPadded(uint.getValue(), 8);
        } else if (obj instanceof Number) {
            long l = ((Number) obj).longValue();
            return toBytes(BigInteger.valueOf(l));
        }
        throw new IllegalArgumentException(obj.getClass().getName());
    }

}
