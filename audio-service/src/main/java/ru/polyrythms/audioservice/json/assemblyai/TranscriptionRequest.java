package ru.polyrythms.audioservice.json.assemblyai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TranscriptionRequest {
    @JsonProperty("audio_url")
    private String audioUrl;
    @JsonProperty("language_code")
    private LanguageCode languageCode;

    public TranscriptionRequest(String audioUrl, LanguageCode languageCode) {
        this.audioUrl = audioUrl;
        this.languageCode = languageCode;
    }
    public enum LanguageCode {
        ru
    }
}
