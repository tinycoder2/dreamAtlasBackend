package com.example.dreamjournal.repository.firestore;

import com.google.cloud.Timestamp;

import java.util.List;

class FirestoreDreamDocument {
    private String text;
    private String mood;
    private String dreamType;
    private List<String> tags;
    private Integer sortOrder;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getDreamType() {
        return dreamType;
    }

    public void setDreamType(String dreamType) {
        this.dreamType = dreamType;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
