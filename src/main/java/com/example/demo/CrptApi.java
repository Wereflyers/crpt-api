package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final long interval;
    private final AtomicInteger counter;
    private final Object lock;
    private final int requestLimit;

    public CrptApi(int requestLimit, TimeUnit timeUnit) {
        this.interval = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.counter = new AtomicInteger(requestLimit);
        this.lock = new Object();

        Thread resetCounterThread = new Thread(this::resetCounter);
        resetCounterThread.setDaemon(true);
        resetCounterThread.start();
    }

    public void createDocument(InputDocument document, String signature) {
        synchronized (lock) {
            while (counter.get() <= 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            try {
                HttpResponse response = sendRequest(document, signature);
                processResponse(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
            counter.decrementAndGet();
        }
    }

    private void resetCounter() {
        while (true) {
            synchronized (lock) {
                counter.set(requestLimit);
                lock.notifyAll();
            }
            sleep(interval);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpResponse sendRequest(InputDocument document, String signature) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(URL);
        String json = serializeToJson(document);

        post.addHeader("Content-Type", "application/json; charset=UTF-8");
        post.addHeader("Accept", "application/json");
        post.addHeader("signature", signature);
        post.setEntity(new StringEntity(json));
        return httpClient.execute(post);
    }

    private void processResponse(HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == 200) {
            System.out.println(response.getEntity().toString());
        } else throw new IllegalArgumentException("Not proper request");
    }


    private String serializeToJson(InputDocument document) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(document);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class InputDocument {
        String description;
        @JsonProperty("doc_id")
        String docId;
        @JsonProperty("doc_status")
        String docStatus;
        @JsonProperty("doc_type")
        DocType docType;
        @JsonProperty("import_request")
        Boolean importRequest;
        @JsonProperty("owner_inn")
        String ownerInn;
        @JsonProperty("participant_inn")
        String participantInn;
        @JsonProperty("producer_inn")
        String producerInn;
        @DateTimeFormat(pattern = "yyyy-MM-dd", iso = DateTimeFormat.ISO.DATE)
        @JsonProperty("production_date")
        String productionDate;
        @JsonProperty("production_type")
        String productionType;
        List<Product> products;
        @DateTimeFormat(pattern = "yyyy-MM-dd", iso = DateTimeFormat.ISO.DATE)
        @JsonProperty("reg_date")
        String regDate;
        @JsonProperty("reg_number")
        String regNumber;

        public enum DocType {
            LP_INTRODUCE_GOODS
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @ToString
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public class Product {
        @JsonProperty("certificate_document")
        String certificateDocument;
        @DateTimeFormat(pattern = "yyyy-MM-dd", iso = DateTimeFormat.ISO.DATE)
        @JsonProperty("certificate_document_date")
        String certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        String ownerInn;
        @JsonProperty("producer_inn")
        String producerInn;
        @DateTimeFormat(pattern = "yyyy-MM-dd", iso = DateTimeFormat.ISO.DATE)
        @JsonProperty("production_datte")
        LocalDate productionDate;
        @JsonProperty("tnved_code")
        String tnvedCode;
        @JsonProperty("uit_code")
        String uitCode;
        @JsonProperty("uitu_code")
        String uituCode;
    }
}
