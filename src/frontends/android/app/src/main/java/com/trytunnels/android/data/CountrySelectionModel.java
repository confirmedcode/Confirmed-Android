package com.trytunnels.android.data;

/**
 * Created by jeff on 12/9/17.
 */

public class CountrySelectionModel {
    private String text;
    private Integer imageId;
    private String endpoint;

    public CountrySelectionModel() {
    }

    public CountrySelectionModel(String text, Integer imageId, String endpoint) {
        this.text = text;
        this.imageId = imageId;
        this.endpoint = endpoint;
    }

    public String getText() {
        return text;
    }

    public Integer getImageId() {
        return imageId;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
