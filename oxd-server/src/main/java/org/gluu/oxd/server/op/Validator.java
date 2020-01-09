package org.gluu.oxd.server.op;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.JwkClient;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.ECDSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jws.AbstractJwsSigner;
import org.gluu.oxauth.model.jws.ECDSASigner;
import org.gluu.oxauth.model.jws.HMACSigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.jwt.JwtClaimName;
import org.gluu.oxauth.model.jwt.JwtHeaderName;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2017
 */

public class Validator {

    private static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    private final OpenIdConfigurationResponse discoveryResponse;
    private AbstractJwsSigner jwsSigner;
    private JwsSignerObject jwsSignerObject;

    public Validator(JwsSignerObject jwsSignerObject, OpenIdConfigurationResponse discoveryResponse) {
        Preconditions.checkNotNull(jwsSignerObject.getIdToken());
        Preconditions.checkNotNull(discoveryResponse);

        this.discoveryResponse = discoveryResponse;
        this.jwsSignerObject = jwsSignerObject;
        this.jwsSigner = createJwsSigner(jwsSignerObject, discoveryResponse);
    }

    public void validateAccessToken(String accessToken) {
        if (!Strings.isNullOrEmpty(accessToken)) {
            String atHash = jwsSignerObject.getIdToken().getClaims().getClaimAsString("at_hash");
            if (Strings.isNullOrEmpty(atHash)) {
                LOG.warn("Skip access_token validation because corresponding id_token does not have at_hash claim. access_token: " + accessToken + ", id_token: " + jwsSignerObject.getIdToken());
                return;
            }
            if (!jwsSigner.validateAccessToken(accessToken, jwsSignerObject.getIdToken())) {
                LOG.trace("Hash from id_token does not match hash of the access_token (at_hash). access_token:" + accessToken + ", idToken: " + jwsSignerObject.getIdToken() + ", at_hash:" + atHash);
                throw new HttpException(ErrorResponseCode.INVALID_ACCESS_TOKEN_BAD_HASH);
            }
        }
    }

    public void validateAuthorizationCode(String code) {
        if (!Strings.isNullOrEmpty(code)) {
            if (!jwsSigner.validateAuthorizationCode(code, jwsSignerObject.getIdToken())) {
                throw new HttpException(ErrorResponseCode.INVALID_AUTHORIZATION_CODE_BAD_HASH);
            }
        }
    }

    public static AbstractJwsSigner createJwsSigner(JwsSignerObject jwsSignerObject, OpenIdConfigurationResponse discoveryResponse) {
        final String algorithm = jwsSignerObject.getIdToken().getHeader().getClaimAsString(JwtHeaderName.ALGORITHM);
        final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(algorithm);
        final String kid = jwsSignerObject.getIdToken().getHeader().getClaimAsString(JwtHeaderName.KEY_ID);
        final String jwkUrl = discoveryResponse.getJwksUri();

        if (signatureAlgorithm.getFamily() == AlgorithmFamily.RSA) {
            final RSAPublicKey publicKey = jwsSignerObject.getKeyService().getRSAPublicKey(jwkUrl, kid);
            return jwsSignerObject.getOpClientFactory().createRSASigner(signatureAlgorithm, publicKey);
        } else if (signatureAlgorithm.getFamily() == AlgorithmFamily.HMAC) {
            return new HMACSigner(signatureAlgorithm, jwsSignerObject.getSite().getClientSecret());
        } else if (signatureAlgorithm.getFamily() == AlgorithmFamily.EC) {
            ECDSAPublicKey publicKey = JwkClient.getECDSAPublicKey(jwkUrl, kid);
            return new ECDSASigner(signatureAlgorithm, publicKey);
        }
        return null;
    }

    public void validateNonce(StateService stateService) {
        final String nonceFromToken = jwsSignerObject.getIdToken().getClaims().getClaimAsString(JwtClaimName.NONCE);
        if (!stateService.isExpiredObjectPresent(nonceFromToken)) {
            throw new HttpException(ErrorResponseCode.INVALID_NONCE);
        }
    }

    public boolean isIdTokenValid() {
        try {
            validateIdToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void validateIdToken() {
        validateIdToken(null);
    }

    public void validateIdToken(String nonce) {
        try {
            final String issuer = jwsSignerObject.getIdToken().getClaims().getClaimAsString(JwtClaimName.ISSUER);
            final String nonceFromToken = jwsSignerObject.getIdToken().getClaims().getClaimAsString(JwtClaimName.NONCE);
            final String clientId = jwsSignerObject.getSite().getClientId();

            if (!Strings.isNullOrEmpty(nonce) && !nonceFromToken.endsWith(nonce)) {
                LOG.error("ID Token has invalid nonce. Expected nonce: " + nonce + ", nonce from token is: " + nonceFromToken);
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_NONCE);
            }
            //validate audience
            validateAudience(jwsSignerObject.getIdToken(), clientId);

            //validate id_token expire date
            final Date expiresAt = jwsSignerObject.getIdToken().getClaims().getClaimAsDate(JwtClaimName.EXPIRATION_TIME);
            final Date now = new Date();
            if (now.after(expiresAt)) {
                LOG.error("ID Token is expired. (" + expiresAt + " is before " + now + ").");
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_EXPIRED);
            }

            // 1. validate issuer
            if (!issuer.equals(discoveryResponse.getIssuer())) {
                LOG.error("ID Token issuer is invalid. Token issuer: " + issuer + ", discovery issuer: " + discoveryResponse.getIssuer());
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_ISSUER);
            }

            // 2. validate signature
            boolean signature = jwsSigner.validate(jwsSignerObject.getIdToken());
            if (!signature) {
                final String jwkUrl = discoveryResponse.getJwksUri();
                final String kid = jwsSignerObject.getIdToken().getHeader().getClaimAsString(JwtHeaderName.KEY_ID);

                jwsSignerObject.getKeyService().refetchKey(jwkUrl, kid);

                AbstractJwsSigner signerWithRefreshedKey = createJwsSigner(jwsSignerObject, discoveryResponse);
                signature = signerWithRefreshedKey.validate(jwsSignerObject.getIdToken());

                if (!signature) {
                    LOG.error("ID Token signature is invalid.");
                    throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_SIGNATURE);
                } else {
                    this.jwsSigner = signerWithRefreshedKey;
                }
            }
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_UNKNOWN);
        }
    }

    //Added this method so that we can write the test cases
    public static void validateAudience(Jwt idToken, String clientId) {

        final String audienceFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUDIENCE);
        if (!clientId.equalsIgnoreCase(audienceFromToken)) {
            List<String> audAsList = idToken.getClaims().getClaimAsStringList(JwtClaimName.AUDIENCE);
            if (audAsList != null) {
                if (!audAsList.stream().anyMatch(aud -> clientId.equalsIgnoreCase(aud))) {
                    LOG.error("ID Token has invalid audience (string list). Expected audience: " + clientId + ", audience from token is: " + audAsList);
                    throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUDIENCE);
                }
                //If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.
                if (audAsList.size() > 1) {
                    String azpFromToken = idToken.getClaims().getClaimAsString(JwtClaimName.AUTHORIZED_PARTY);
                    //If an azp (authorized party) Claim is present, the Client SHOULD verify that its client_id is the Claim Value. If present, it MUST contain the OAuth 2.0 Client ID of this party.
                    if (!Strings.isNullOrEmpty(azpFromToken) && !azpFromToken.equalsIgnoreCase(clientId)) {
                        LOG.error("ID Token has invalid authorized party (string list). Expected authorized party: " + clientId + ", authorized party from token is: " + azpFromToken);
                        throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUTHORIZED_PARTY);
                    }
                }
            }

            // somehow fetching string list does not return actual list, so we do this ugly trick to compare single valued array, more details in #178
            /*boolean equalsWithSingleValuedArray = ("[\"" + clientId + "\"]").equalsIgnoreCase(audienceFromToken);
            if (!equalsWithSingleValuedArray) {
                LOG.error("ID Token has invalid audience (single valued array). Expected audience: " + clientId + ", audience from token is: " + audienceFromToken);
                throw new HttpException(ErrorResponseCode.INVALID_ID_TOKEN_BAD_AUDIENCE);
            }*/
        }
    }

    public Jwt getIdToken() {
        return jwsSignerObject.getIdToken();
    }
}
