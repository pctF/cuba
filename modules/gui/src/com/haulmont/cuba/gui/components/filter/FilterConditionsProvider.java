/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.cuba.gui.components.filter;


import com.haulmont.cuba.core.app.PersistenceManagerService;
import com.haulmont.cuba.core.global.AppBeans;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

@Component(FilterConditionsProvider.NAME)
public class FilterConditionsProvider {

    public static final String NAME = "cuba_FilterConditionsProvider";

    public FilterConditions getFilterConditions(String entityName) {
        PersistenceManagerService persistenceManager = AppBeans.get(PersistenceManagerService.NAME);
        String storeType = persistenceManager.getStoreType(entityName);
        Collection<FilterConditions> allFilterConditions = AppBeans.getAll(FilterConditions.class).values();
        return allFilterConditions.stream()
                .filter(filterConditions -> Objects.equals(storeType, filterConditions.getStoreDialect()))
                .findFirst()
                .orElse(defaultFilterConditions());
    }

    protected FilterConditions defaultFilterConditions() {
        return AppBeans.get(JPQLFilterConditions.NAME);
    }
}
