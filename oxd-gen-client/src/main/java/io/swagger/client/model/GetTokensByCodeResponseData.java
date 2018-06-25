package io.swagger.client.model;

import java.util.Objects;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.client.model.GetTokensByCodeResponseDataIdTokenClaims;
import java.io.IOException;

/**
 * GetTokensByCodeResponseData
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-06-25T16:29:00.516Z")
public class GetTokensByCodeResponseData {
  @SerializedName("access_token")
  private String accessToken = null;

  @SerializedName("expires_in")
  private Integer expiresIn = null;

  @SerializedName("id_token")
  private String idToken = null;

  @SerializedName("refresh_token")
  private String refreshToken = null;

  @SerializedName("id_token_claims")
  private GetTokensByCodeResponseDataIdTokenClaims idTokenClaims = null;

  public GetTokensByCodeResponseData accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

   /**
   * Get accessToken
   * @return accessToken
  **/
  @ApiModelProperty(example = "b75434ff-f465-4b70-92e4-b7ba6b6c58f2", required = true, value = "")
  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public GetTokensByCodeResponseData expiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
    return this;
  }

   /**
   * Get expiresIn
   * @return expiresIn
  **/
  @ApiModelProperty(example = "299", required = true, value = "")
  public Integer getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(Integer expiresIn) {
    this.expiresIn = expiresIn;
  }

  public GetTokensByCodeResponseData idToken(String idToken) {
    this.idToken = idToken;
    return this;
  }

   /**
   * Get idToken
   * @return idToken
  **/
  @ApiModelProperty(example = "eyJraWQiOiI5MTUyNTU1Ni04YmIwLTQ2MzYtYTFhYy05ZGVlNjlhMDBmYWUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJp", required = true, value = "")
  public String getIdToken() {
    return idToken;
  }

  public void setIdToken(String idToken) {
    this.idToken = idToken;
  }

  public GetTokensByCodeResponseData refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

   /**
   * Get refreshToken
   * @return refreshToken
  **/
  @ApiModelProperty(example = "33d7988e-6ffb-4fe5-8c2a-0e158691d446", required = true, value = "")
  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public GetTokensByCodeResponseData idTokenClaims(GetTokensByCodeResponseDataIdTokenClaims idTokenClaims) {
    this.idTokenClaims = idTokenClaims;
    return this;
  }

   /**
   * Get idTokenClaims
   * @return idTokenClaims
  **/
  @ApiModelProperty(required = true, value = "")
  public GetTokensByCodeResponseDataIdTokenClaims getIdTokenClaims() {
    return idTokenClaims;
  }

  public void setIdTokenClaims(GetTokensByCodeResponseDataIdTokenClaims idTokenClaims) {
    this.idTokenClaims = idTokenClaims;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetTokensByCodeResponseData getTokensByCodeResponseData = (GetTokensByCodeResponseData) o;
    return Objects.equals(this.accessToken, getTokensByCodeResponseData.accessToken) &&
        Objects.equals(this.expiresIn, getTokensByCodeResponseData.expiresIn) &&
        Objects.equals(this.idToken, getTokensByCodeResponseData.idToken) &&
        Objects.equals(this.refreshToken, getTokensByCodeResponseData.refreshToken) &&
        Objects.equals(this.idTokenClaims, getTokensByCodeResponseData.idTokenClaims);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessToken, expiresIn, idToken, refreshToken, idTokenClaims);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetTokensByCodeResponseData {\n");
    
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    expiresIn: ").append(toIndentedString(expiresIn)).append("\n");
    sb.append("    idToken: ").append(toIndentedString(idToken)).append("\n");
    sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
    sb.append("    idTokenClaims: ").append(toIndentedString(idTokenClaims)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

