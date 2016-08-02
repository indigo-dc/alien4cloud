package alien4cloud.rest.plugin;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import alien4cloud.plugin.IPluginLoadingCallback;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Map rest services to the rest dispatcher.
 */
@Slf4j
@Component
public class PluginRestMapper implements IPluginLoadingCallback, HandlerMapping, PriorityOrdered {
    private List<HandlerMapping> handlerMappings = Lists.newArrayList();
    @Resource
    private PluginManager pluginManager;
    private Map<String, RequestMappingHandlerMapping> pluginMappings = Maps.newHashMap();

    @Override
    public synchronized void onPluginLoaded(ManagedPlugin managedPlugin) {
        mapContext(managedPlugin);
    }

    @Override
    public synchronized void onPluginClosed(ManagedPlugin managedPlugin) {
        RequestMappingHandlerMapping mapping = this.pluginMappings.remove(managedPlugin.getPlugin().getDescriptor().getId());
        for (int i = 0; i < this.handlerMappings.size(); i++) {
            // identity check and remove if this is the mapping associated with the plugin to close.
            if (this.handlerMappings.get(i) == mapping) {
                this.handlerMappings.remove(i);
                return;
            }
        }
    }

    private void mapContext(ManagedPlugin managedPlugin) {
        // dispatcher context is ready after the plugin mapper actually.
        RequestMappingHandlerMapping mapper = new RequestMappingHandlerMapping();
        mapper.setApplicationContext(managedPlugin.getPluginContext());
        mapper.afterPropertiesSet();
        this.handlerMappings.add(mapper);
        this.pluginMappings.put(managedPlugin.getPlugin().getDescriptor().getId(), mapper);
    }

    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        for (HandlerMapping hm : this.handlerMappings) {
            if (log.isTraceEnabled()) {
                log.trace("Testing handler map [" + hm + "]");
            }
            HandlerExecutionChain handler = hm.getHandler(request);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
    }
}