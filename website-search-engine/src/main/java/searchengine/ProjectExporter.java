package searchengine;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.io.BufferedWriter;

public class ProjectExporter {

    public static void main(String[] args) {
        Path projectPath = Paths.get("C:/Users/user/IdeaProjects/SpringFramework/website-search-engine/src/main/java");

        try {
            // собираем все .java файлы
            List<Path> files;
            try (var paths = Files.walk(projectPath)) {
                files = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".java"))
                        .filter(p -> !p.toString().contains(FileSystems.getDefault().getSeparator() + "build" + FileSystems.getDefault().getSeparator()))
                        .filter(p -> !p.toString().contains(FileSystems.getDefault().getSeparator() + "target" + FileSystems.getDefault().getSeparator()))
                        .sorted(Comparator.comparing(p -> p.toAbsolutePath().toString()))
                        .toList();
            }

            if (files.isEmpty()) {
                System.err.println("Файлы не найдены в " + projectPath.toAbsolutePath());
                return;
            }

            // группируем по подпапкам
            Map<String, List<Path>> grouped = new LinkedHashMap<>();
            for (Path file : files) {
                Path rel = projectPath.relativize(file.getParent());
                String groupName = rel.toString().replace(File.separator, "/");
                if (groupName.isEmpty()) {
                    groupName = "searchengine"; // корневой пакет
                } else {
                    groupName = "searchengine/" + groupName;
                }
                grouped.computeIfAbsent(groupName, k -> new ArrayList<>()).add(file);
            }

            // пишем каждый пакет в отдельный файл
            for (Map.Entry<String, List<Path>> entry : grouped.entrySet()) {
                String fileName = entry.getKey() + ".txt";
                Path outputFile = Paths.get(fileName.replace("/", "_")); // чтобы ОС не путалась
                try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                    for (Path file : entry.getValue()) {
                        writer.write("=== " + file.toAbsolutePath() + " ===\n");
                        for (String line : Files.readAllLines(file)) {
                            writer.write(line);
                            writer.write("\n");
                        }
                        writer.write("\n\n");
                    }
                }
                System.out.println("Экспортирован пакет: " + entry.getKey() + " -> " + outputFile);
            }

            System.out.println("Экспорт завершён! Всего пакетов: " + grouped.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
