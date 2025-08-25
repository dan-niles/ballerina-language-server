/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.search;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.ballerina.flowmodelgenerator.core.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.modelgenerator.commons.SearchResult;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LineRange;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles the search command for memory managers.
 *
 * @since 1.1.0
 */
public class MemoryManagerSearchCommand extends SearchCommand {
    private static final Gson GSON = new Gson();
    private List<Item> cachedMemoryManagers;
    private final String orgName;

    public MemoryManagerSearchCommand(Project project, LineRange position, Map<String, String> queryMap) {
        super(project, position, queryMap);
        orgName = queryMap.get("orgName");
    }

    @Override
    protected List<Item> defaultView() {
        return getMemoryManagers();
    }

    @Override
    protected List<Item> search() {
        List<Item> memoryManagers = getMemoryManagers();
        if (memoryManagers.isEmpty() || !(memoryManagers.getFirst() instanceof Category memoryManagerCategory)) {
            return memoryManagers;
        }

        List<Item> stores = memoryManagerCategory.items();

        List<Item> matchingStores = stores.stream()
                .filter(item -> item instanceof AvailableNode availableNode &&
                        (orgName == null || availableNode.codedata().org().equalsIgnoreCase(orgName)) &&
                        (query == null || availableNode.metadata().label().toLowerCase().contains(query.toLowerCase())))
                .toList();

        stores.clear();
        stores.addAll(matchingStores);

        return List.of(memoryManagerCategory);
    }

    @Override
    protected Map<String, List<SearchResult>> fetchPopularItems() {
        return Collections.emptyMap();
    }

    @Override
    public JsonArray execute() {
        List<Item> items = (query.isEmpty() && orgName == null) ? defaultView() : search();
        return GSON.toJsonTree(items).getAsJsonArray();
    }

    private List<Item> getMemoryManagers() {
        if (cachedMemoryManagers == null) {
            cachedMemoryManagers = List.copyOf(LocalIndexCentral.getInstance().getMemoryManagers());
        }
        return cachedMemoryManagers;
    }
}
