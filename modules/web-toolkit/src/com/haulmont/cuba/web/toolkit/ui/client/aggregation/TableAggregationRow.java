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

package com.haulmont.cuba.web.toolkit.ui.client.aggregation;

import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.haulmont.cuba.web.toolkit.ui.client.Tools;
import com.haulmont.cuba.web.toolkit.ui.client.tableshared.TableWidget;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ComputedStyle;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.VScrollTable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Special aggregation row for {@link com.haulmont.cuba.web.toolkit.ui.client.table.CubaScrollTableWidget} and
 * {@link com.haulmont.cuba.web.toolkit.ui.client.treetable.CubaTreeTableWidget}
 */
public class TableAggregationRow extends Panel {

    protected boolean initialized = false;

    protected char[] aligns;
    protected Element tr;

    protected TableWidget tableWidget;

    protected BiConsumer<Integer, String> totalAggregationInputHandler;
    protected List<AggregationInputFieldInfo> inputsList;

    public TableAggregationRow(TableWidget tableWidget) {
        this.tableWidget = tableWidget;

        setElement(Document.get().createDivElement());

        getElement().setClassName(tableWidget.getStylePrimaryName() + "-arow");
        getElement().getStyle().setOverflow(Overflow.HIDDEN);
    }

    @Override
    public Iterator<Widget> iterator() {
        return Collections.<Widget>emptyList().iterator();
    }

    @Override
    public boolean remove(Widget child) {
        return false;
    }

    public void updateFromUIDL(UIDL uidl) {
        if (getElement().hasChildNodes()) {
            getElement().removeAllChildren();
        }

        aligns = tableWidget.getHead().getColumnAlignments();

        if (uidl.getChildCount() > 0) {
            final Element table = DOM.createTable();
            table.setAttribute("cellpadding", "0");
            table.setAttribute("cellspacing", "0");

            final Element tBody = DOM.createTBody();
            tr = DOM.createTR();

            tr.setClassName(tableWidget.getStylePrimaryName() + "-arow-row");

            if (inputsList != null && !inputsList.isEmpty()) {
                inputsList.clear();
            }

            addCellsFromUIDL(uidl);

            tBody.appendChild(tr);
            table.appendChild(tBody);
            getElement().appendChild(table);
        }

        initialized = getElement().hasChildNodes();
    }

    public void rollbackInputFieldValue(int columnIndex) {
        for (AggregationInputFieldInfo info : inputsList) {
            if (info.getColumnIndex() == columnIndex) {
                info.getInputElement().setValue(info.oldValue);
                break;
            }
        }
    }

    protected void addCellsFromUIDL(UIDL uidl) {
        int colIndex = 0;
        final Iterator cells = uidl.getChildIterator();
        while (cells.hasNext() && colIndex < tableWidget.getVisibleColOrder().length) {
            String columnId = tableWidget.getVisibleColOrder()[colIndex];

            if (addSpecificCell(columnId, colIndex)) {
                colIndex++;
                continue;
            }

            final Object cell = cells.next();

            String style = "";
            if (uidl.hasAttribute("style-" + columnId)) {
                style = uidl.getStringAttribute("style-" + columnId);
            }

            boolean sorted = tableWidget.getHead().getHeaderCell(colIndex).isSorted();

            if (isEditableAggr(uidl, colIndex)) {
                addCellWithField((String) cell, aligns[colIndex], style, sorted, colIndex);
            } else if (cell instanceof String) {
                addCell((String) cell, aligns[colIndex], style, sorted);
            }

            final String colKey = tableWidget.getColKeyByIndex(colIndex);
            int colWidth;
            if ((colWidth = tableWidget.getColWidth(colKey)) > -1) {
                tableWidget.setColWidth(colIndex, colWidth, false);
            }

            colIndex++;
        }
    }

    //todo do without class cast
    protected boolean isEditableAggr(UIDL uidl, int colIndex) {
        UIDL colUidl = uidl.getChildByTagName("editableAggregationColumns");
        if (colUidl == null) {
            return false;
        }
        Iterator iterator = colUidl.getChildIterator();
        while (iterator.hasNext()) {
            Object col = iterator.next();
            if (col instanceof String) {
                int colIdx = Integer.parseInt((String) col);
                if (colIdx == colIndex) {
                    return true;
                }
            }
        }
        return false;
    }

    // Extension point for GroupTable divider column
    protected boolean addSpecificCell(String columnId, int colIndex) {
        return false;
    }

    protected void addCellWithField(String text, char align, String style, boolean sorted, int colIndex) {
        final TableCellElement td = DOM.createTD().cast();
        final DivElement container = DOM.createDiv().cast();
        container.setClassName(tableWidget.getStylePrimaryName() + "-cell-wrapper" + " " + "widget-container");

        setAlign(align, container);

        InputElement inputElement = DOM.createInputText().cast();
        inputElement.setValue(text);
        inputElement.addClassName("v-textfield v-widget");
        Style elemStyle = inputElement.getStyle();
        elemStyle.setWidth(100, Style.Unit.PCT);

        container.appendChild(inputElement);

        if (inputsList == null) {
            inputsList = new ArrayList<>();
        }
        inputsList.add(new AggregationInputFieldInfo(text, colIndex, inputElement));

        DOM.sinkEvents(inputElement, Event.ONCHANGE);

        td.setClassName(tableWidget.getStylePrimaryName() + "-cell-content");
        td.addClassName(tableWidget.getStylePrimaryName() + "-aggregation-cell");
        td.appendChild(container);
        tr.appendChild(td);
    }

    protected void addCell(String text, char align, String style, boolean sorted) {
        final TableCellElement td = DOM.createTD().cast();

        final Element container = DOM.createDiv();
        container.setClassName(tableWidget.getStylePrimaryName() + "-cell-wrapper");

        td.setClassName(tableWidget.getStylePrimaryName() + "-cell-content");

        td.addClassName(tableWidget.getStylePrimaryName() + "-aggregation-cell");

        if (style != null && !style.equals("")) {
            td.addClassName(tableWidget.getStylePrimaryName() + "-cell-content-" + style);
        }

        if (sorted) {
            td.addClassName(tableWidget.getStylePrimaryName() + "-cell-content-sorted");
        }

        container.setInnerText(text);

        setAlign(align, container);

        td.appendChild(container);
        tr.appendChild(td);

        Tools.textSelectionEnable(td, tableWidget.isTextSelectionEnabled());
    }

    protected void setAlign(char align, final Element container) {
        // CAUTION: copied from VScrollTableRow
        switch (align) {
            case VScrollTable.ALIGN_CENTER:
                container.getStyle().setProperty("textAlign", "center");
                break;
            case VScrollTable.ALIGN_LEFT:
                container.getStyle().setProperty("textAlign", "left");
                break;
            case VScrollTable.ALIGN_RIGHT:
            default:
                container.getStyle().setProperty("textAlign", "right");
                break;
        }
    }

    public void setCellWidth(int cellIx, int width) {
        // CAUTION: copied from VScrollTableRow with small changes
        final Element cell = DOM.getChild(tr, cellIx);
        Style wrapperStyle = cell.getFirstChildElement().getStyle();
        int wrapperWidth = width;
        if (BrowserInfo.get().isWebkit()
                || BrowserInfo.get().isOpera10()) {
                    /*
                     * Some versions of Webkit and Opera ignore the width
                     * definition of zero width table cells. Instead, use 1px
                     * and compensate with a negative margin.
                     */
            if (width == 0) {
                wrapperWidth = 1;
                wrapperStyle.setMarginRight(-1, Style.Unit.PX);
            } else {
                wrapperStyle.clearMarginRight();
            }
        }
        wrapperStyle.setPropertyPx("width", wrapperWidth);
        cell.getStyle().setPropertyPx("width", width);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setHorizontalScrollPosition(int scrollLeft) {
        getElement().setPropertyInt("scrollLeft", scrollLeft);
    }

    public TableWidget getTableWidget() {
        return tableWidget;
    }

    // CAUTION copied from com.vaadin.client.ui.VScrollTable.VScrollTableBody.VScrollTableRow.getRealCellWidth
    public double getRealCellWidth(int colIdx) {
        if (colIdx >= tr.getChildCount()) {
            return -1;
        }

        Element cell = DOM.getChild(tr, colIdx);
        ComputedStyle cs = new ComputedStyle(cell);

        return cs.getWidth() + cs.getPaddingWidth() + cs.getBorderWidth();
    }

    public void setTotalAggregationInputHandler(BiConsumer<Integer, String> totalAggregationInputHandler) {
        this.totalAggregationInputHandler = totalAggregationInputHandler;
    }

    protected Integer getColumnIndex(Element input) {
        for (AggregationInputFieldInfo info : inputsList) {
            if (info.getInputElement().isOrHasChild(input)) {
                return info.getColumnIndex();
            }
        }
        return null;
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

        final int type = DOM.eventGetType(event);
        if (type == Event.ONCHANGE && totalAggregationInputHandler != null) {
            Element element = Element.as(event.getEventTarget());
            Integer columnIndex = getColumnIndex(element);
            if (columnIndex != null) {
                InputElement input = element.cast();
                totalAggregationInputHandler.accept(columnIndex, input.getValue());
            }
        }
    }
}