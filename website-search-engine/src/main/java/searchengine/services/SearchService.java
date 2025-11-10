package searchengine.services;

import searchengine.dto.response.SearchResponseDTO;

public interface SearchService {
    SearchResponseDTO search(String query, String site, int offset, int limit);
}
