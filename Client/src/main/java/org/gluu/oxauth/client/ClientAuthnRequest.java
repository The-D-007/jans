package org.gluu.oxauth.client;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtType;
import org.gluu.oxauth.model.token.ClientAssertionType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public abstract class ClientAuthnRequest extends BaseRequest {

    private static final Logger LOG = Logger.getLogger(ClientAuthnRequest.class);

    private SignatureAlgorithm algorithm;
    private String sharedKey;
    private String audience;
    private AbstractCryptoProvider cryptoProvider;
    private String keyId;

    public ClientAuthnRequest() {
    }

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public void appendClientAuthnToQuery(StringBuilder queryStringBuilder) throws UnsupportedEncodingException {
        if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (getAuthUsername() != null && !getAuthUsername().isEmpty()) {
                queryStringBuilder.append("&");
                queryStringBuilder.append("client_id=").append(
                        URLEncoder.encode(getAuthUsername(), "UTF-8"));
            }
            if (getAuthPassword() != null && !getAuthPassword().isEmpty()) {
                queryStringBuilder.append("&");
                queryStringBuilder.append("client_secret=").append(
                        URLEncoder.encode(getAuthPassword(), "UTF-8"));
            }
        } else if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            queryStringBuilder.append("&client_assertion_type=").append(
                    URLEncoder.encode(ClientAssertionType.JWT_BEARER.toString(), "UTF-8"));
            queryStringBuilder.append("&");
            queryStringBuilder.append("client_assertion=").append(getClientAssertion());
        }
    }

    public String getClientAssertion() {
        if (cryptoProvider == null) {
            LOG.error("Crypto provider is not specified");
            return null;
        }

        if (algorithm == null) {
            algorithm = SignatureAlgorithm.HS256;
        }

        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        Date issuedAt = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date expirationTime = calendar.getTime();

        Jwt clientAssertion = new Jwt();
        // Header
        clientAssertion.getHeader().setType(JwtType.JWT);
        clientAssertion.getHeader().setAlgorithm(algorithm);
        if (StringUtils.isNotBlank(keyId)) {
            clientAssertion.getHeader().setKeyId(keyId);
        }

        // Claims
        clientAssertion.getClaims().setIssuer(getAuthUsername());
        clientAssertion.getClaims().setSubjectIdentifier(getAuthUsername());
        clientAssertion.getClaims().setAudience(audience);
        clientAssertion.getClaims().setJwtId(UUID.randomUUID());
        clientAssertion.getClaims().setExpirationTime(expirationTime);
        clientAssertion.getClaims().setIssuedAt(issuedAt);

        // Signature
        try {
            if (sharedKey == null) {
                sharedKey = getAuthPassword();
            }
            String signature = cryptoProvider.sign(clientAssertion.getSigningInput(), keyId, sharedKey, algorithm);
            clientAssertion.setEncodedSignature(signature);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return clientAssertion.toString();
    }
}
