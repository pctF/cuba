/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.haulmont.cuba.web.gui.components;

import com.haulmont.bali.events.Subscription;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.gui.components.ResizableTextArea;
import com.haulmont.cuba.web.widgets.CubaResizableTextAreaWrapper;
import com.haulmont.cuba.web.widgets.CubaTextArea;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Component;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;
import org.springframework.beans.factory.InitializingBean;

import java.util.function.Consumer;

public class WebResizableTextArea<V> extends WebAbstractTextArea<CubaTextArea, V>
        implements ResizableTextArea<V>, InitializingBean {

    protected CubaResizableTextAreaWrapper wrapper;
    protected boolean settingsEnabled = true;
    protected boolean settingsChanged = false;

    public WebResizableTextArea() {
        component = createComponent();
        attachValueChangeListener(component);

        wrapper = new CubaResizableTextAreaWrapper(component);
        wrapper.addResizeListener((oldWidth, oldHeight, width, height) -> {
            ResizeEvent e = new ResizeEvent(this, oldWidth, width, oldHeight, height);
            publish(ResizeEvent.class, e);

            settingsChanged = true;
        });
    }

    @Override
    protected void componentValueChanged(String prevComponentValue, String newComponentValue, boolean isUserOriginated) {
        wrapper.markAsDirty();

        super.componentValueChanged(prevComponentValue, newComponentValue, isUserOriginated);
    }

    protected CubaTextArea createComponent() {
        return new CubaTextArea() {
            @Override
            public void setComponentError(ErrorMessage componentError) {
                if (componentError instanceof UserError) {
                    super.setComponentError(componentError);
                } else {
                    wrapper.setComponentError(componentError);
                }
            }
        };
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initComponent(component);
    }

    protected void initComponent(CubaTextArea component) {
        component.setValueChangeMode(ValueChangeMode.BLUR);
    }

    @Override
    public Component getComposition() {
        return wrapper;
    }

    @Override
    public boolean isResizable() {
        return getResizableDirection() != ResizeDirection.NONE;
    }

    @Override
    public void setResizable(boolean resizable) {
        ResizeDirection value = resizable ? ResizeDirection.BOTH : ResizeDirection.NONE;
        setResizableDirection(value);
    }

    @Override
    public String getCaption() {
        return wrapper.getCaption();
    }

    @Override
    public void setCaption(String caption) {
        wrapper.setCaption(caption);
    }

    @Override
    public String getDescription() {
        return wrapper.getDescription();
    }

    @Override
    public void setDescription(String description) {
        wrapper.setDescription(description);
    }

    @Override
    public boolean isRequired() {
        return wrapper.isRequired();
    }

    @Override
    public void setRequired(boolean required) {
        wrapper.setRequired(required);
    }

    @Override
    protected void setEditableToComponent(boolean editable) {
        wrapper.setEditable(editable);
    }

    @Override
    public String getRequiredMessage() {
        return wrapper.getRequiredError();
    }

    @Override
    public void setRequiredMessage(String msg) {
        wrapper.setRequiredError(msg);
    }

    @Override
    public void applySettings(Element element) {
        if (isSettingsEnabled() && isResizable()) {
            String width = element.attributeValue("width");
            String height = element.attributeValue("height");
            if (StringUtils.isNotEmpty(width)
                    && StringUtils.isNotEmpty(height)) {
                setWidth(width);
                setHeight(height);
            }
        }
    }

    @Override
    public boolean saveSettings(Element element) {
        if (!isSettingsEnabled() || !isResizable()) {
            return false;
        }

        if (!settingsChanged) {
            return false;
        }

        String width = String.valueOf(getWidth()) + wrapper.getWidthUnits().toString();
        String height = String.valueOf(getHeight()) + wrapper.getHeightUnits().toString();
        element.addAttribute("width", width);
        element.addAttribute("height", height);

        return true;
    }

    @Override
    public boolean isSettingsEnabled() {
        return settingsEnabled;
    }

    @Override
    public void setSettingsEnabled(boolean settingsEnabled) {
        this.settingsEnabled = settingsEnabled;
    }

    @Override
    public CaseConversion getCaseConversion() {
        return CaseConversion.valueOf(component.getCaseConversion().name());
    }

    @Override
    public void setCaseConversion(CaseConversion caseConversion) {
        com.haulmont.cuba.web.widgets.CaseConversion widgetCaseConversion =
                com.haulmont.cuba.web.widgets.CaseConversion.valueOf(caseConversion.name());
        component.setCaseConversion(widgetCaseConversion);
    }

    @Override
    public ResizeDirection getResizableDirection() {
        return WebWrapperUtils.toResizeDirection(wrapper.getResizableDirection());
    }

    @Override
    public Subscription addResizeListener(Consumer<ResizeEvent> listener) {
        return getEventHub().subscribe(ResizeEvent.class, listener);
    }

    @Override
    public void removeResizeListener(Consumer<ResizeEvent> listener) {
        unsubscribe(ResizeEvent.class, listener);
    }

    @Override
    public void setResizableDirection(ResizeDirection direction) {
        Preconditions.checkNotNullArgument(direction);
        wrapper.setResizableDirection(WebWrapperUtils.toVaadinResizeDirection(direction));
    }
}