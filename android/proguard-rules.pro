-dontobfuscate
-dontoptimize
-dontpreverify
-dontshrink

-dontwarn com.sun.mail.handlers.handler_base
-dontwarn java.awt.**
-dontwarn java.beans.Beans
-dontwarn javax.naming.**
-dontwarn javax.security.sasl.**
-dontwarn javax.security.auth.callback.NameCallback

-dontwarn net.sf.ntru.**

-keepclassmembers class i2p.bote.crypto.ECUtils {
  public static java.security.spec.ECParameterSpec getParameters(java.lang.String);
  public static byte[] encodePoint(java.security.spec.ECParameterSpec, java.security.spec.ECPoint, boolean);
  public static java.security.spec.ECPoint decodePoint(java.security.spec.EllipticCurve, byte[]);
}
