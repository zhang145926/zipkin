/**
 * Copyright 2015-2016 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.storage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import zipkin.Constants;

import static org.assertj.core.api.Assertions.assertThat;
import static zipkin.TraceKeys.HTTP_METHOD;

public class QueryRequestTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void serviceNameCanBeNull() {
    assertThat(QueryRequest.builder().build().serviceName)
        .isNull();
  }

  @Test
  public void serviceNameCantBeEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("serviceName was empty");

    QueryRequest.builder().serviceName("").build();
  }

  @Test
  public void spanNameCantBeEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("spanName was empty");

    QueryRequest.builder().serviceName("foo").spanName("").build();
  }

  @Test
  public void annotationCantBeEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("annotation was empty");

    QueryRequest.builder().serviceName("foo").addAnnotation("").build();
  }

  /**
   * Particularly in the case of cassandra, indexing boundary annotations isn't fruitful work, and
   * not helpful to users. Nevertheless we should ensure an unlikely caller gets an exception.
   */
  @Test
  public void annotationCantBeCore() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("queries cannot be refined by core annotations: sr");

    QueryRequest.builder().serviceName("foo").addAnnotation(Constants.SERVER_RECV).build();
  }

  @Test
  public void binaryAnnotationKeyCantBeEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("binary annotation key was empty");

    QueryRequest.builder().serviceName("foo").addBinaryAnnotation("", "bar").build();
  }

  @Test
  public void binaryAnnotationValueCantBeEmpty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("binary annotation value for foo was empty");

    QueryRequest.builder().serviceName("foo").addBinaryAnnotation("foo", "").build();
  }

  @Test
  public void endTsMustBePositive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("endTs should be positive, in epoch microseconds: was 0");

    QueryRequest.builder().serviceName("foo").endTs(0L).build();
  }

  @Test
  public void limitMustBePositive() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("limit should be positive: was 0");

    QueryRequest.builder().serviceName("foo").limit(0).build();
  }

  @Test
  public void annotationQuery_roundTrip() {
    String annotationQuery = "http.method=GET and error";

    QueryRequest request =
        QueryRequest.builder().serviceName("security-service").parseAnnotationQuery(annotationQuery).build();

    assertThat(request.binaryAnnotations)
        .containsEntry(HTTP_METHOD, "GET")
        .hasSize(1);
    assertThat(request.annotations)
        .containsExactly(Constants.ERROR);

    assertThat(request.toAnnotationQuery())
        .isEqualTo(annotationQuery);
  }

  @Test
  public void annotationQuery_missingValue() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("binary annotation value for http.method was empty");

    String annotationQuery = "http.method=";

    QueryRequest request =
        QueryRequest.builder().serviceName("security-service").parseAnnotationQuery(annotationQuery).build();

    assertThat(request.annotations)
        .containsExactly(HTTP_METHOD);
  }

  @Test
  public void toAnnotationQueryWhenNoInputIsNull() {
    QueryRequest request = QueryRequest.builder().serviceName("security-service").build();

    assertThat(request.toAnnotationQuery())
        .isNull();
  }
}
