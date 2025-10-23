package ru.polyrythms.audioservice.json.assemblyai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionResponse {
    private String id;
}
