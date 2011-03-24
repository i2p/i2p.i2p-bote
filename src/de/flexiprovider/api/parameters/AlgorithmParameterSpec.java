package de.flexiprovider.api.parameters;

/**
 * A (transparent) specification of cryptographic parameters.
 * <p>
 * This interface contains no methods or constants. Its only purpose is to group
 * (and provide type safety for) all parameter specifications. All parameter
 * specifications must implement this interface.
 */
public interface AlgorithmParameterSpec extends
	java.security.spec.AlgorithmParameterSpec {

    // empty

}
