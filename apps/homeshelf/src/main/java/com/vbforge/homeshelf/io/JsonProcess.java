package com.vbforge.homeshelf.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbforge.homeshelf.MediaItem;

import java.io.File;
import java.util.List;

public class JsonProcess implements DataProcess {
    private final File file;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonProcess(File file) { this.file = file; }

    @Override
    public void save(List<MediaItem> items) throws Exception {
        mapper.writeValue(file, items);
    }

    @Override
    public List<MediaItem> load() throws Exception {
        if (!file.exists()) return List.of();
        return mapper.readValue(file, new TypeReference<List<MediaItem>>() {});
    }
}