package searchengine.services;

import searchengine.dto.search.SearchResponseDTO;

public interface SearchService {
    SearchResponseDTO search(String query, String site, int offset, int limit);
}
