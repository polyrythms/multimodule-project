package ru.polyrythms.audioservice.json.assemblyai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// DTO классы
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResponse {
    private String upload_url;

    public String getUploadUrl() {
        return upload_url;
    }
}
