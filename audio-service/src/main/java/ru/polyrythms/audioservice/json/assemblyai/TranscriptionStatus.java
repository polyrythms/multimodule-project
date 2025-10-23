package ru.polyrythms.audioservice.json.assemblyai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionStatus {
    private String status;
    private String text;
    private String error;
    private Double confidence;
}
