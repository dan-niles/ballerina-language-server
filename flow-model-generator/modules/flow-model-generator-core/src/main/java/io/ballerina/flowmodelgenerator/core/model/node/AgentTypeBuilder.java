/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.flowmodelgenerator.core.model.NodeKind;

/**
 * Represents the declaration of a user-defined agent class that includes
 * {@code *ai:FixedReturnAgent} or {@code *ai:InferredReturnAgent}.
 * <p>
 * Distinct from {@link AgentBuilder}, which is reserved for the built-in
 * {@code ai:Agent} whose constructor exposes {@code systemPrompt}/{@code tools}/
 * {@code model}/{@code memory} directly. For custom agents those values live
 * inside the class body, not the constructor — so this builder defers to the
 * generic class-init rendering for now. A richer edit experience will be added
 * here when prompt/tool/model editing is wired through to the class source.
 *
 * @since 1.5.1
 */
public class AgentTypeBuilder extends ClassInitBuilder {

    private static final String AGENT_LABEL = "Agent";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.AGENT_TYPE;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(AGENT_LABEL);
        codedata().node(NodeKind.AGENT_TYPE).symbol("init");
    }
}
