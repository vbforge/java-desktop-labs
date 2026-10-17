package com.vbforge.homeshelf.io;

import com.vbforge.homeshelf.MediaItem;
import java.util.List;

public interface DataProcess {
    void save(List<MediaItem> items) throws Exception;
    List<MediaItem> load() throws Exception;
}