/*
 * jans-api-server
 * jans-api-server
 *
 * OpenAPI spec version: 4.2
 * Contact: yuriyz@gluu.org
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package io.swagger.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * Condition
 */


public class Condition {
  @SerializedName("httpMethods")
  private List<String> httpMethods = new ArrayList<String>();

  @SerializedName("scopes")
  private List<String> scopes = new ArrayList<String>();

  @SerializedName("scope_expression")
  private List<String> scopeExpression = new ArrayList<String>();

  @SerializedName("ticketScopes")
  private List<String> ticketScopes = new ArrayList<String>();

  public Condition httpMethods(List<String> httpMethods) {
    this.httpMethods = httpMethods;
    return this;
  }

  public Condition addHttpMethodsItem(String httpMethodsItem) {
    this.httpMethods.add(httpMethodsItem);
    return this;
  }

   /**
   * Get httpMethods
   * @return httpMethods
  **/
  @Schema(required = true, description = "")
  public List<String> getHttpMethods() {
    return httpMethods;
  }

  public void setHttpMethods(List<String> httpMethods) {
    this.httpMethods = httpMethods;
  }

  public Condition scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public Condition addScopesItem(String scopesItem) {
    this.scopes.add(scopesItem);
    return this;
  }

   /**
   * Get scopes
   * @return scopes
  **/
  @Schema(required = true, description = "")
  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  public Condition scopeExpression(List<String> scopeExpression) {
    this.scopeExpression = scopeExpression;
    return this;
  }

  public Condition addScopeExpressionItem(String scopeExpressionItem) {
    this.scopeExpression.add(scopeExpressionItem);
    return this;
  }

   /**
   * Get scopeExpression
   * @return scopeExpression
  **/
  @Schema(required = true, description = "")
  public List<String> getScopeExpression() {
    return scopeExpression;
  }

  public void setScopeExpression(List<String> scopeExpression) {
    this.scopeExpression = scopeExpression;
  }

  public Condition ticketScopes(List<String> ticketScopes) {
    this.ticketScopes = ticketScopes;
    return this;
  }

  public Condition addTicketScopesItem(String ticketScopesItem) {
    this.ticketScopes.add(ticketScopesItem);
    return this;
  }

   /**
   * Get ticketScopes
   * @return ticketScopes
  **/
  @Schema(required = true, description = "")
  public List<String> getTicketScopes() {
    return ticketScopes;
  }

  public void setTicketScopes(List<String> ticketScopes) {
    this.ticketScopes = ticketScopes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Condition condition = (Condition) o;
    return Objects.equals(this.httpMethods, condition.httpMethods) &&
        Objects.equals(this.scopes, condition.scopes) &&
        Objects.equals(this.scopeExpression, condition.scopeExpression) &&
        Objects.equals(this.ticketScopes, condition.ticketScopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(httpMethods, scopes, scopeExpression, ticketScopes);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Condition {\n");
    
    sb.append("    httpMethods: ").append(toIndentedString(httpMethods)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("    scopeExpression: ").append(toIndentedString(scopeExpression)).append("\n");
    sb.append("    ticketScopes: ").append(toIndentedString(ticketScopes)).append("\n");
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
