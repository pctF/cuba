/*
 * Copyright (c) 2008-2017 Haulmont.
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
 */

package com.haulmont.cuba.web.widgets;

import com.vaadin.event.Action;
import com.vaadin.event.ActionManager;
import com.vaadin.event.ShortcutListener;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.shared.Registration;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.LegacyComponent;

import java.util.Map;

public class CubaGridLayout extends GridLayout implements Action.Container, LegacyComponent {

    protected ActionManager actionManager;

    public CubaGridLayout() {
        setHideEmptyRowsAndColumns(true);
    }

    @Override
    public void addActionHandler(Action.Handler actionHandler) {
        getActionManager().addActionHandler(actionHandler);
        markAsDirty();
    }

    @Override
    public void removeActionHandler(Action.Handler actionHandler) {
        if (actionManager != null) {
            actionManager.removeActionHandler(actionHandler);
            markAsDirty();
        }
    }

    @Override
    public Registration addShortcutListener(ShortcutListener listener) {
        getActionManager().addAction(listener);
        return () -> getActionManager().removeAction(listener);
    }

    @Override
    public void removeShortcutListener(ShortcutListener listener) {
        getActionManager().removeAction(listener);
    }

    @Override
    protected ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = new ActionManager(this);
        }
        return actionManager;
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        if (actionManager != null) {
            actionManager.paintActions(null, target);
        }
    }

    @Override
    public void changeVariables(Object source, Map<String, Object> variables) {
        if (actionManager != null) {
            actionManager.handleActions(variables, this);
        }
    }
}