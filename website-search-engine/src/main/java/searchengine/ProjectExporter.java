package searchengine;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;
import java.io.BufferedWriter;

public class ProjectExporter {
    public static void main(String[] args) {
        // Корневая директория проекта
        Path projectPath = Paths.get("C:/Users/user/IdeaProjects/SpringFramework/website-search-engine");
        // Файл для сохранения всего содержимого
        Path outputFile = Paths.get("project_dump.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile);
             Stream<Path> paths = Files.walk(projectPath)) {

            paths.filter(Files::isRegularFile) // только файлы
                    .filter(p -> p.toString().endsWith(".java")
                            || p.toString().endsWith(".xml")
                            || p.toString().endsWith(".md")
                            || p.toString().endsWith(".properties")
                            || p.toString().endsWith(".sql")
                            || p.toString().endsWith(".html")
                            || p.toString().endsWith(".gradle"))
                    .forEach(file -> writeFileContent(file, writer));

            System.out.println("Экспорт завершён! Содержимое проекта сохранено в " + outputFile.toAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeFileContent(Path file, BufferedWriter writer) {
        try {
            writer.write("=== " + file.toString() + " ===\n");
            String content = Files.readString(file);
            writer.write(content);
            writer.write("\n\n");
        } catch (IOException e) {
            System.err.println("Ошибка при обработке файла: " + file);
            e.printStackTrace();
        }
    }
}
