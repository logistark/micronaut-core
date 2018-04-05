/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package io.micronaut.upload

import groovy.json.JsonSlurper
import io.micronaut.AbstractMicronautSpec
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.multipart.MultipartBody
import io.reactivex.Flowable

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class UploadSpec extends AbstractMicronautSpec {

    @Override
    Map<String, Object> getConfiguration() {
        ['micronaut.server.multipart.maxFileSize': '1KB']
    }

    void "test simple in-memory file upload with JSON"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.APPLICATION_JSON_TYPE, '{"title":"Foo"}'.bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receiveJson", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()

        then:
        response.code() == HttpStatus.OK.code
        response.getBody().get() == 'bar: Data{title=\'Foo\'}'

    }

    void "test simple in-memory file upload with invalid JSON"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.APPLICATION_JSON_TYPE, '{"title":"Foo"'.bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receiveJson", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE),
                String
        ))

        HttpResponse<String> response = flowable.blockingFirst()

        then:
        response.code() == HttpStatus.BAD_REQUEST.code

        when:
        def json = new JsonSlurper().parseText(response.getBody().get())

        then:
        json.message.contains("Failed to convert argument [data]")
        json.path == "/data"

    }

    void "test simple in-memory file upload "() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.TEXT_PLAIN_TYPE, 'some data'.bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()

        then:
        response.code() == HttpStatus.OK.code
        response.getBody().get() == 'bar: some data'
    }

    void "test file upload with wrong argument name for file"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("datax", "data.json", MediaType.TEXT_PLAIN_TYPE, 'some data'.bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()
        def body = response.getBody().get()
        def json = new JsonSlurper().parseText(body)

        then:
        response.code() == HttpStatus.BAD_REQUEST.code
        json.message == "Required argument [String data] not specified"
    }

    void "test file upload with wrong argument name for simple part"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.APPLICATION_JSON_TYPE, 'some data'.bytes)
                .addPart("titlex", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()
        def body = response.getBody().get()
        def json = new JsonSlurper().parseText(body)

        then:
        response.code() == HttpStatus.BAD_REQUEST.code
        json.message == "Required argument [String title] not specified"
    }

    void "test file upload with missing argument for simple part"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.TEXT_PLAIN_TYPE, 'some data'.bytes)
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()
        def body = response.getBody().get()
        def json = new JsonSlurper().parseText(body)

        then:
        response.code() == HttpStatus.BAD_REQUEST.code
        json.message == "Required argument [String title] not specified"
    }

    void "test file upload with missing argument for file part"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()
        def body = response.getBody().get()
        def json = new JsonSlurper().parseText(body)

        then:
        response.code() == HttpStatus.BAD_REQUEST.code
        json.message == "Required argument [String data] not specified"
    }

    void "test file upload to byte array"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.TEXT_PLAIN_TYPE,'some data'.bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receiveBytes", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()

        then:
        response.code() == HttpStatus.OK.code
        response.getBody().get() == 'bar: 9'
    }

    void "test simple in-memory file upload exceeds size"() {
        given:
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("data", "data.json", MediaType.TEXT_PLAIN_TYPE, ('some data' * 1000).bytes)
                .addPart("title", "bar")
                .build()

        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receivePlain", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()

        then:
        response.code() == HttpStatus.REQUEST_ENTITY_TOO_LARGE.code
        response.reason() == 'Request Entity Too Large'
        def body = response.getBody().get()

        when:
        def json = new JsonSlurper().parseText(body)

        then:
        json.message.contains("exceeds the maximum content length [1024]")
    }

    void "test upload CompletedFileUpload object"() {
        given:
        def data = '{"title":"Test"}'
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("title", "bar")
                .addPart("data", "data.json", MediaType.APPLICATION_JSON_TYPE, data.bytes)
                .build()


        when:
        Flowable<HttpResponse<String>> flowable = Flowable.fromPublisher(client.exchange(
                HttpRequest.POST("/upload/receiveCompletedFileUpload", requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.TEXT_PLAIN_TYPE),
                String
        ))
        HttpResponse<String> response = flowable.blockingFirst()
        def result = response.getBody().get()

        then:
        response.code() == HttpStatus.OK.code
        result == 'data.json: 16'
    }
}
