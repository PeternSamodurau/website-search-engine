package com.example.booksManagement.client.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VolumeItem {
    private VolumeInfo volumeInfo;
}
