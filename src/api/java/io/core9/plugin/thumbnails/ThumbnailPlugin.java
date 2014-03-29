package io.core9.plugin.thumbnails;

import io.core9.core.executor.Executor;
import io.core9.core.plugin.Core9Plugin;
import io.core9.plugin.admin.AdminPlugin;
import io.core9.plugin.server.VirtualHostProcessor;

public interface ThumbnailPlugin extends Core9Plugin, Executor, AdminPlugin, VirtualHostProcessor {

}
