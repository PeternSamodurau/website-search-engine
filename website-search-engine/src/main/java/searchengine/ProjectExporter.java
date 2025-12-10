package searchengine;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.io.BufferedWriter;

public class ProjectExporter {
    private static final int MAX_LINES = 10_000; // лимит строк на одну часть

    public static void main(String[] args) {
        Path projectPath = Paths.get("C:/Users/user/IdeaProjects/SpringFramework/website-search-engine");
        int partCounter = 1;
        int currentLines = 0;

        try {
            // Собираем список файлов один раз и сортируем для стабильного порядка
            List<Path> files;
            try (var paths = Files.walk(projectPath)) {
                files = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String s = p.toString().toLowerCase();
                            return s.endsWith(".java")
                                    || s.endsWith(".xml")
                                    || s.endsWith(".md")
                                    || s.endsWith(".properties")
                                    || s.endsWith(".sql")
                                    || s.endsWith(".html")
                                    || s.endsWith(".gradle");
                        })
                        // Исключим типичные директории сборки
                        .filter(p -> !p.toString().contains(FileSystems.getDefault().getSeparator() + "build" + FileSystems.getDefault().getSeparator()))
                        .filter(p -> !p.toString().contains(FileSystems.getDefault().getSeparator() + "target" + FileSystems.getDefault().getSeparator()))
                        .sorted(Comparator.comparing(Path::toString))
                        .toList();
            }

            BufferedWriter writer = createWriter(partCounter);

            for (Path file : files) {
                // Читаем строки заранее, чтобы оценить «сколько займёт»
                List<String> lines = Files.readAllLines(file);
                int needed = 1 /* заголовок */ + lines.size() + 2 /* пустые строки после файла */;

                // Если не помещается целиком — переключаемся на новую часть
                if (currentLines + needed > MAX_LINES) {
                    writer.close();
                    partCounter++;
                    writer = createWriter(partCounter);
                    currentLines = 0;
                }

                // Пишем заголовок с абсолютным путём для однозначности
                writer.write("=== " + file.toAbsolutePath() + " ===\n");
                currentLines++;

                for (String line : lines) {
                    writer.write(line);
                    writer.write("\n");
                    currentLines++;
                }

                writer.write("\n\n");
                currentLines += 2;
            }

            writer.close();
            System.out.println("Экспорт завершён! Создано частей: " + partCounter);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedWriter createWriter(int partNumber) throws IOException {
        Path outputFile = Paths.get("project_dump_part" + partNumber + ".txt");
        return Files.newBufferedWriter(outputFile);
    }
}
