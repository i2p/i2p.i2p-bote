package i2p.bote.crypto;

import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;

/**
 * A wrapper around the BouncyCastle EC classes.
 * <p/>
 * On Android, this is replaced by a wrapper around the SpongyCastle
 * EC classes.
 */
public class ECUtils {
    public static ECParameterSpec getParameters(String curveName) {
        X9ECParameters params = NISTNamedCurves.getByName(curveName);
        return new ECNamedCurveSpec(curveName, params.getCurve(), params.getG(), params.getN(), params.getH(), null);
    }

    public static byte[] encodePoint(
            ECParameterSpec ecSpec,
            ECPoint point,
            boolean withCompression) {
        org.bouncycastle.math.ec.ECPoint bcPoint = EC5Util.convertPoint(ecSpec, point, withCompression);
        return bcPoint.getEncoded(withCompression);
    }

    public static ECPoint decodePoint(
            EllipticCurve curve,
            byte[] encoded) {
        return ECPointUtil.decodePoint(curve, encoded);
    }
}
