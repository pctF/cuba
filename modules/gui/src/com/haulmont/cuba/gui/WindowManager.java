/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 26.01.2009 11:14:00
 * $Id$
 */
package com.haulmont.cuba.gui;

import com.haulmont.bali.util.ReflectionHelper;
import com.haulmont.cuba.core.app.ServerConfig;
import com.haulmont.cuba.core.app.TemplateHelper;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AccessDeniedException;
import com.haulmont.cuba.core.global.ConfigProvider;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.impl.DatasourceFactoryImpl;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.xml.ParametersHelper;
import com.haulmont.cuba.gui.xml.data.DsContextLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import com.haulmont.cuba.gui.xml.layout.loaders.ComponentLoaderContext;
import com.haulmont.cuba.security.app.UserSettingService;
import com.haulmont.cuba.security.entity.PermissionType;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.*;
import java.util.List;

/**
 * GenericUI class intended for creating and opening application screens.
 */
public abstract class WindowManager {

    /**
     * How to open a screen: {@link #NEW_TAB}, {@link #THIS_TAB}, {@link #DIALOG}
     */
    public enum OpenType {
        /** In new tab for TABBED mode, replace current screen for SINGLE mode */
        NEW_TAB,
        /** On top of the current conversation stack */
        THIS_TAB,
        /** In modal dialog */
        DIALOG
    }

    private DataService defaultDataService;
    private UserSettingService settingService;

    public synchronized DataService getDefaultDataService() {
        if (defaultDataService == null) {
            defaultDataService = createDefaultDataService();
        }
        return defaultDataService;
    }

    public synchronized UserSettingService getSettingService() {
        if (settingService == null) {
            settingService = ServiceLocator.lookup(UserSettingService.JNDI_NAME);
        }
        return settingService;
    }

    protected abstract DataService createDefaultDataService();

    public abstract Collection<Window> getOpenWindows();

    protected Window createWindow(WindowInfo windowInfo, Map<String, Object> params, LayoutLoaderConfig layoutConfig) {
        checkPermission(windowInfo);

        String templatePath = windowInfo.getTemplate();
        Document document = LayoutLoader.parseDescriptor(getClass().getResourceAsStream(templatePath), params);

        final Element element = document.getRootElement();

        MetadataHelper.deployViews(element);

        final DsContext dsContext = loadDsContext(element);
        final ComponentLoaderContext componentLoaderContext = new ComponentLoaderContext(dsContext, params);

        final Window window = loadLayout(windowInfo.getTemplate(), element, componentLoaderContext, layoutConfig);

        window.setId(windowInfo.getId());

        componentLoaderContext.setFrame(window);
        initDatasources(window, dsContext, params);

        final Window windowWrapper = wrapByCustomClass(window, element, params);
        componentLoaderContext.setFrame(windowWrapper);
        componentLoaderContext.executeLazyTasks();

        if (ConfigProvider.getConfig(ServerConfig.class).getTestMode()) {
            initDebugIds(window);
        }

        return windowWrapper;
    }

    protected void initDebugIds(final Window window) {
        ComponentsHelper.walkComponents(window, new ComponentVisitor() {
            public void visit(Component component, String name) {
                component.setDebugId(window.getId() + "." + name);
            }
        });
    }

    private void checkPermission(WindowInfo windowInfo) {
        boolean permitted = UserSessionClient.getUserSession().isScreenPermitted(
                AppConfig.getInstance().getClientType(),
                windowInfo.getId()
        );
        if (!permitted)
            throw new AccessDeniedException(PermissionType.SCREEN, windowInfo.getId());
    }

    protected void initDatasources(final Window window, DsContext dsContext, Map<String, Object> params) {
        window.setDsContext(dsContext);

        for (Datasource ds : dsContext.getAll()) {
            if (ds instanceof DatasourceImplementation) {
                ((DatasourceImplementation) ds).initialized();
            }
        }

        dsContext.setWindowContext(new FrameContext(window, params));
    }

    protected Window loadLayout(String descriptorPath, Element rootElement, ComponentLoader.Context context, LayoutLoaderConfig layoutConfig) {
        final LayoutLoader layoutLoader = new LayoutLoader(context, createComponentFactory(), layoutConfig);
        layoutLoader.setLocale(getLocale());
        if (!StringUtils.isEmpty(descriptorPath)) {
            String path = descriptorPath.replaceAll("/", ".");
            path = path.substring(1, path.lastIndexOf("."));

            layoutLoader.setMessagesPack(path);
        }

        final Window window = (Window) layoutLoader.loadComponent(rootElement, null);
        return window;
    }

    protected DsContext loadDsContext(Element element) {
        DataService dataService;

        String dataserviceClass = element.attributeValue("dataservice");
        if (StringUtils.isEmpty(dataserviceClass)) {
            final Element dataserviceElement = element.element("dataservice");
            if (dataserviceElement != null) {
                dataserviceClass = dataserviceElement.getText();
                if (StringUtils.isEmpty(dataserviceClass)) {
                    throw new IllegalStateException("Can't find dataservice class name");
                }
                dataService = createDataservice(dataserviceClass, dataserviceElement);
            } else {
                dataService = getDefaultDataService();
            }
        } else {
            dataService = createDataservice(dataserviceClass, null);
        }

        final DsContextLoader dsContextLoader = new DsContextLoader(new DatasourceFactoryImpl(), dataService);
        final DsContext dsContext = dsContextLoader.loadDatasources(element.element("dsContext"), null);

        return dsContext;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    protected DataService createDataservice(String dataserviceClass, Element element) {
        DataService dataService;
        
        final Class<Object> aClass = ReflectionHelper.getClass(dataserviceClass);
        try {
            dataService = (DataService) aClass.newInstance();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return dataService;
    }

    protected Window createWindow(WindowInfo windowInfo, Map params) {
        final Window window;
        try {
            window = (Window) windowInfo.getScreenClass().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        window.setId(windowInfo.getId());
        try {
            ReflectionHelper.invokeMethod(window, "init", params);
        } catch (NoSuchMethodException e) {
            // Do nothing
        }
        return window;
    }

    public <T extends Window> T openWindow(WindowInfo windowInfo, WindowManager.OpenType openType, Map<String, Object> params)
    {
        params = createParametersMap(windowInfo, params);
        String template = windowInfo.getTemplate();
        if (template != null) {
            //noinspection unchecked
            Window window = createWindow(windowInfo, params, LayoutLoaderConfig.getWindowLoaders());
            window.setId(windowInfo.getId());

            String caption = loadCaption(window, params);
            showWindow(window, caption, openType);

            return (T)window;
        } else {
            Class screenClass = windowInfo.getScreenClass();
            if (screenClass != null)
            {
                //noinspection unchecked
                Window window = createWindow(windowInfo, params);
                window.setId(windowInfo.getId());

                String caption = loadCaption(window, params);
                showWindow(window, caption, openType);

                return (T)window;
            } else
                return null;
        }
    }

    protected String loadCaption(Window window, Map<String, Object> params) {
        String caption = window.getCaption();
        if (!StringUtils.isEmpty(caption)) {
            caption = TemplateHelper.processTemplate(caption, params);
        } else {
            caption = (String) params.get("param$caption");
            if (StringUtils.isEmpty(caption)) {
                String msgPack = window.getMessagesPack();
                if (msgPack != null) {
                    caption = MessageProvider.getMessage(msgPack, "caption");
                    if (!"caption".equals(caption)) {
                        caption = TemplateHelper.processTemplate(caption, params);
                    }
                }
            } else {
                caption = TemplateHelper.processTemplate(caption, params);
            }
        }
        window.setCaption(caption);

        return caption;
    }

    public <T extends Window> T openEditor(WindowInfo windowInfo, Entity item, OpenType openType,
                                           Datasource parentDs)
    {
        //noinspection unchecked
        return (T)openEditor(windowInfo, item, openType, Collections.<String, Object>emptyMap(), parentDs);
    }

    public <T extends Window> T openEditor(WindowInfo windowInfo, Entity item, OpenType openType) {
        //noinspection unchecked
        return (T)openEditor(windowInfo, item, openType, Collections.<String, Object>emptyMap());
    }

    public <T extends Window> T openEditor(WindowInfo windowInfo, Entity item, OpenType openType, Map<String, Object> params) {
        //noinspection unchecked
        return (T)openEditor(windowInfo, item, openType, params, null);
    }

    public <T extends Window> T openEditor(WindowInfo windowInfo, Entity item,
                                           OpenType openType, Map<String, Object> params,
                                           Datasource parentDs)
    {
        params = createParametersMap(windowInfo, params);
        params.put("param$item", item instanceof Datasource ? ((Datasource) item).getItem() : item);

        String template = windowInfo.getTemplate();
        Window window;
        if (template != null) {
            window = createWindow(windowInfo, params, LayoutLoaderConfig.getEditorLoaders());
        } else {
            Class windowClass = windowInfo.getScreenClass();
            if (windowClass != null) {
                window = createWindow(windowInfo, params);
                if (!(window instanceof Window.Editor)) {
                    throw new IllegalStateException(
                            String.format("Class %s does't implement Window.Editor interface", windowClass));
                }
            } else {
                throw new IllegalStateException("Invalid WindowInfo: " + windowInfo);
            }
        }
        ((Window.Editor) window).setParentDs(parentDs);
        ((Window.Editor) window).setItem(item);

        String caption = loadCaption(window, params);
        showWindow(window, caption, openType);

        //noinspection unchecked
        return (T) window;
    }

    public <T extends Window> T openLookup(
            WindowInfo windowInfo, Window.Lookup.Handler handler,
                OpenType openType, Map<String, Object> params)
    {
        params = createParametersMap(windowInfo, params);

        String template = windowInfo.getTemplate();
        Window window;

        if (template != null) {
            window = createWindow(windowInfo, params, LayoutLoaderConfig.getLookupLoaders());

            final Element element = ((Component.HasXmlDescriptor) window).getXmlDescriptor();
            final String lookupComponent = element.attributeValue("lookupComponent");
            if (!StringUtils.isEmpty(lookupComponent)) {
                final Component component = window.getComponent(lookupComponent);
                ((Window.Lookup) window).setLookupComponent(component);
            }
        } else {
            Class windowClass = windowInfo.getScreenClass();
            if (windowClass != null) {
                window = createWindow(windowInfo, params);
                if (!(window instanceof Window.Lookup)) {
                    throw new IllegalStateException(
                            String.format("Class %s does't implement Window.Lookup interface", windowClass));
                }
            } else {
                throw new IllegalStateException("Invalid WindowInfo: " + windowInfo);
            }
        }
        window.setId(windowInfo.getId());
        ((Window.Lookup) window).setLookupHandler(handler);

        String caption = loadCaption(window, params);
        showWindow(window, caption, openType);

        //noinspection unchecked
        return (T) window;
    }

    protected Map<String, Object> createParametersMap(WindowInfo windowInfo, Map<String, Object> params) {
        final Map<String, Object> map = new HashMap<String, Object>(params.size());

        final Element element = windowInfo.getDescriptor();
        if (element != null) {
            final Element paramsElement = element.element("params");
            if (paramsElement != null) {
                @SuppressWarnings({"unchecked"})
                final List<Element> paramElements = paramsElement.elements("param");
                for (Element paramElement : paramElements) {
                    final String name = paramElement.attributeValue("name");
                    final String value = paramElement.attributeValue("value");

                    map.put(ParametersHelper.ParameterInfo.Type.PARAM.getPrefix() + "$" + name, value);
                }
            }
        }


        for (Map.Entry<String, Object> entry : params.entrySet()) {
            map.put(ParametersHelper.ParameterInfo.Type.PARAM.getPrefix() + "$" + entry.getKey(), entry.getValue());
        }

        return map;
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T extends Window> T openWindow(WindowInfo windowInfo, OpenType openType) {
        //noinspection unchecked
        return (T)openWindow(windowInfo, openType, Collections.<String, Object>emptyMap());
    }

    public <T extends Window> T openLookup(WindowInfo windowInfo, Window.Lookup.Handler handler, OpenType openType) {
        //noinspection unchecked
        return (T)openLookup(windowInfo, handler, openType, Collections.<String, Object>emptyMap());
    }

    protected abstract void showWindow(Window window, String caption, OpenType openType);

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract Locale getLocale();
    protected abstract ComponentsFactory createComponentFactory();

    protected Window wrapByCustomClass(Window window, Element element, Map<String, Object> params) {
        Window res = window;
        final String screenClass = element.attributeValue("class");
        if (!StringUtils.isBlank(screenClass)) {
            final Class<Window> aClass = ReflectionHelper.getClass(screenClass);
            res = ((WrappedWindow) window).wrapBy(aClass);

            try {
                ReflectionHelper.invokeMethod(res, "init", params);
            } catch (NoSuchMethodException e) {
                // do nothing
            }
            return res;
        } else {
            return res;
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public abstract void showMessageDialog(String title, String message, IFrame.MessageType messageType);
    public abstract void showOptionDialog(String title, String message, IFrame.MessageType messageType, Action[] actions);
}
