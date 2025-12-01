package searchengine.config;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LemmaConfiguration {

    @Bean("russianLuceneMorphology")
    public LuceneMorphology russianLuceneMorphology() {
        try {
            return new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create RussianLuceneMorphology bean", e);
        }
    }

    @Bean("englishLuceneMorphology")
    public LuceneMorphology englishLuceneMorphology() {
        try {
            return new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create EnglishLuceneMorphology bean", e);
        }
    }
}
