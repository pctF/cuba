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

package com.haulmont.cuba.gui.components;

/**
 * Simple list select component.
 *
 * @param <V> value type: single type or {@code Collection<I>}
 * @param <I> item type
 */
public interface OptionsList<V, I> extends OptionsField<V, I>, Component.Focusable {
    String NAME = "optionsList";

    boolean isMultiSelect();
    void setMultiSelect(boolean multiselect);

    /**
     * Sets visibility for first null element in list.
     */
    void setNullOptionVisible(boolean nullOptionVisible);
    /**
     * @return true if first null element is visible.
     */
    boolean isNullOptionVisible();
}