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

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.web.App;

public class ShowLinkAction extends BaseAction {
    public static final String ACTION_ID = "showLink";

    public interface Handler {
        String makeLink(Entity entity);
    }

    protected CollectionDatasource ds;
    protected Handler handler;

    public ShowLinkAction(CollectionDatasource ds, Handler handler) {
        super(ACTION_ID);

        this.ds = ds;
        this.handler = handler;

        Messages messages = AppBeans.get(Messages.NAME);
        setCaption(messages.getMainMessage("table.showLinkAction"));
    }

    @Override
    public void actionPerform(com.haulmont.cuba.gui.components.Component component) {
        if (ds == null) {
            return;
        }

        Messages messages = AppBeans.get(Messages.NAME);
        WindowManager wm = App.getInstance().getWindowManager();
        wm.showMessageDialog(
                messages.getMainMessage("table.showLinkAction"),
                compileLink(ds),
                Frame.MessageType.CONFIRMATION_HTML
        );
    }

    protected String compileLink(CollectionDatasource ds) {
        StringBuilder sb = new StringBuilder();

        Messages messages = AppBeans.get(Messages.NAME);
        sb.append(messages.getMainMessage("table.showLinkAction.link")).append("<br/>");
        sb.append("<textarea class=\"c-table-showlink-textarea\" autofocus=\"true\" readonly=\"true\">").
                append(handler.makeLink(ds.getItem()).replace("&", "&amp")).append("</textarea>");

        return sb.toString();
    }
}