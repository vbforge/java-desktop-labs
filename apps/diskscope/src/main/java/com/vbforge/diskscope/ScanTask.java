package com.vbforge.diskscope;

import javafx.concurrent.Task;
import java.nio.file.Path;

public class ScanTask extends Task<DirectoryScanner.ScanResult> {

    private final Path path;

    public ScanTask(Path path) {
        this.path = path;
    }

    @Override
    protected DirectoryScanner.ScanResult call() throws Exception {
        return DirectoryScanner.scan(path);
    }
}