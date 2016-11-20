package i2p.bote.crypto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

/**
 * A wrapper around the BouncyCastle EC classes.
 * <p/>
 * On Android, the SpongyCastle EC classes are used instead.
 */
public class ECUtils {
    private static String getPackage() {
        try {
            Class<?> clazz = Class.forName("org.spongycastle.jce.ECPointUtil");
            return "org.spongycastle";
        } catch (ClassNotFoundException e) {
            return "org.bouncycastle";
        }
    }

    public static ECParameterSpec getParameters(String curveName) {
        String pkg = getPackage();
        try {
            Class<?> ncClazz = Class.forName(pkg + ".asn1.nist.NISTNamedCurves");
            Class<?> ecpClazz = Class.forName(pkg + ".asn1.x9.X9ECParameters");
            Class<?> ncsClazz = Class.forName(pkg + ".jce.spec.ECNamedCurveSpec");
            Class<?> cvClazz = Class.forName(pkg + ".math.ec.ECCurve");
            Class<?> ptClazz = Class.forName(pkg + ".math.ec.ECPoint");

            Constructor<?> ncsCtor = ncsClazz.getConstructor(
                    String.class, cvClazz, ptClazz,
                    BigInteger.class, BigInteger.class, byte[].class);

            Method getByName = ncClazz.getDeclaredMethod("getByName", String.class);
            Method getCurve = ecpClazz.getDeclaredMethod("getCurve");
            Method getG = ecpClazz.getDeclaredMethod("getG");
            Method getN = ecpClazz.getDeclaredMethod("getN");
            Method getH = ecpClazz.getDeclaredMethod("getH");

            Object params = getByName.invoke(null, curveName);
            return (ECParameterSpec) ncsCtor.newInstance(
                    curveName,
                    getCurve.invoke(params),
                    getG.invoke(params),
                    getN.invoke(params),
                    getH.invoke(params),
                    null);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("getParameters() failed", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("getParameters() failed", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("getParameters() failed", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("getParameters() failed", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("getParameters() failed", e);
        }
    }

    public static byte[] encodePoint(
            ECParameterSpec ecSpec,
            ECPoint point,
            boolean withCompression) {
        String pkg = getPackage();
        try {
            Class<?> utilClazz = Class.forName(pkg + ".jcajce.provider.asymmetric.util.EC5Util");
            Class<?> ptClazz = Class.forName(pkg + ".math.ec.ECPoint");

            Method convertPoint = utilClazz.getDeclaredMethod(
                    "convertPoint", ECParameterSpec.class, ECPoint.class, boolean.class);
            Method getEncoded = ptClazz.getDeclaredMethod("getEncoded");

            Object bcPoint = convertPoint.invoke(null, ecSpec, point, withCompression);
            return (byte[]) getEncoded.invoke(bcPoint);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("encodePoint() failed", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("encodePoint() failed", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("encodePoint() failed", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("encodePoint() failed", e);
        }
    }

    public static ECPoint decodePoint(
            EllipticCurve curve,
            byte[] encoded) {
        String pkg = getPackage();
        try {
            Class<?> utilClazz = Class.forName(pkg + ".jce.ECPointUtil");
            Method decodePoint = utilClazz.getDeclaredMethod(
                    "decodePoint", EllipticCurve.class, byte[].class);
            return (ECPoint) decodePoint.invoke(null, curve, encoded);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("decodePoint() failed", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("decodePoint() failed", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("decodePoint() failed", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("decodePoint() failed", e);
        }
    }
}
