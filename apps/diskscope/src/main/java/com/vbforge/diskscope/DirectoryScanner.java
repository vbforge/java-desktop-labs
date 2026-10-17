package com.vbforge.diskscope;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class DirectoryScanner {

    private static final String[] COLORS = {
        "#f89820", "#6ea8d8", "#3fb950", "#f85149", "#a371f7", "#db6d28", "#388bfd"
    };
    private static final String COLOR_OTHER = "#8b949e";
    private static final String COLOR_FILE = "#30363d";

    public record ScanResult(List<DiskItem> items, int totalFiles, int totalFolders) {}
    private record FolderStats(long size, int files, int folders) {}

    private DirectoryScanner() {}

    public static ScanResult scan(Path root) throws IOException {
        List<DiskItem> children = new ArrayList<>();
        int totalFiles = 0;
        int totalFolders = 1; // Start with the root folder itself

        try (Stream<Path> stream = Files.list(root)) {
            List<Path> paths = stream.toList();
            for (Path p : paths) {
                boolean isDir = Files.isDirectory(p);
                
                if (isDir) {
                    FolderStats fs = calculateFolderStats(p);
                    totalFiles += fs.files();
                    totalFolders += fs.folders();
                    totalFolders++; // Count this subfolder itself
                    
                    if (fs.size() > 0) {
                        children.add(new DiskItem(p.getFileName().toString(), p, fs.size(), true, ""));
                    }
                } else {
                    long size = safeFileSize(p);
                    totalFiles++;
                    if (size > 0) {
                        children.add(new DiskItem(p.getFileName().toString(), p, size, false, COLOR_FILE));
                    }
                }
            }
        }

        children.sort(Comparator.comparingLong(DiskItem::size).reversed());

        List<DiskItem> result = new ArrayList<>();
        long otherSize = 0;
        int folderCount = 0;

        for (DiskItem item : children) {
            if (item.isDirectory()) {
                if (folderCount < 7) {
                    String color = COLORS[folderCount % COLORS.length];
                    result.add(new DiskItem(item.name(), item.path(), item.size(), true, color));
                    folderCount++;
                } else {
                    otherSize += item.size();
                }
            } else {
                result.add(item);
            }
        }

        if (otherSize > 0) {
            result.add(new DiskItem("Other folders", null, otherSize, false, COLOR_OTHER));
        }

        result.sort(Comparator.comparingLong(DiskItem::size).reversed());

        return new ScanResult(result, totalFiles, totalFolders);
    }

    private static long safeFileSize(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    private static FolderStats calculateFolderStats(Path folder) {
        final long[] size = {0};
        final int[] files = {0};
        final int[] folders = {0};
        
        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    folders[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size[0] += attrs.size();
                    files[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        } catch (IOException ignored) { }
        
        return new FolderStats(size[0], files[0], folders[0]);
    }
}