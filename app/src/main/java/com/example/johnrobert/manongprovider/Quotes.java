package com.example.johnrobert.manongprovider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;

public class Quotes {

    private String serviceName, requestId, date, requestUserId;
    private HashMap<String, Integer> quotePrice;
    private Object timestamp;
    private String userId;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public Quotes() {
        if (user != null) {
            userId = user.getUid();
        }
    }

    public Quotes(String userId, String serviceName, String requestId, String date, HashMap<String, Integer> quotePrice, Object timestamp, String requestUserId) {
        this.userId = userId;
        this.serviceName = serviceName;
        this.requestId = requestId;
        this.date = date;
        this.quotePrice = quotePrice;
        this.timestamp = timestamp;
        this.requestUserId = requestUserId;
    }

    public String getRequestUserId() {
        return requestUserId;
    }

    public void setRequestUserId(String requestUserId) {
        this.requestUserId = requestUserId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public HashMap<String, Integer> getQuotePrice() {
        return quotePrice;
    }

    public void setQuotePrice(HashMap<String, Integer> quotePrice) {
        this.quotePrice = quotePrice;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}
