package com.matrix.launcher;

import java.util.List;

/** Only the remote configuration overlay is consumed by Compose. */
final class ThemeFiles {
    private ThemeFiles() {}

    static String path(List<String> segments) {
        return segments != null && segments.size() == 1 && RemoteConfig.FILE_NAME.equals(segments.get(0))
                ? RemoteConfig.FILE_NAME : null;
    }
}
