package i2p.bote.crypto;

import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jce.ECPointUtil;
import org.spongycastle.jce.spec.ECNamedCurveSpec;

import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

/**
 * A wrapper around the SpongyCastle EC classes.
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
        org.spongycastle.math.ec.ECPoint bcPoint = EC5Util.convertPoint(ecSpec, point, withCompression);
        return bcPoint.getEncoded();
    }

    public static ECPoint decodePoint(
            EllipticCurve curve,
            byte[] encoded) {
        return ECPointUtil.decodePoint(curve, encoded);
    }
}
