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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.flowmodelgenerator.core.Constants.DEFAULT_MODEL_PROVIDER;

/**
 * Represents agent node in the flow model.
 *
 * @since 1.0.0
 */
public class AgentBuilder extends CallBuilder {

    private static final String AGENT_LABEL = "Agent";
    private FunctionData.Kind functionKind = FunctionData.Kind.CONNECTOR;

    public static final String PARAMS_TO_HIDE = "paramsToHide";
    public static final String MODEL = "model";
    public static final String TYPE = "type";
    public static final String TOOLS = "tools";
    public static final String SYSTEM_PROMPT = "systemPrompt";
    public static final String MEMORY = "memory";
    public static final String CHECK_ERROR = "checkError";

    public static final String NAME = "name";
    public static final String NAME_LABEL = "Agent Name";
    public static final String NAME_DOC = "A unique identifier for your agent";

    public static final String ROLE = "role";
    public static final String ROLE_LABEL = "Role";
    public static final String ROLE_DOC = "Define the agent's primary function";
    public static final String ROLE_PLACEHOLDER = "e.g., Customer Support Assistant, Sales Advisor, Data Analyst";

    public static final String INSTRUCTION = "instruction";
    public static final String INSTRUCTION_LABEL = "Instructions";
    public static final String INSTRUCTION_DOC = "Describe the agent's persona, tasks, and boundaries";
    public static final String INSTRUCTION_PLACEHOLDER = "e.g., You are a friendly assistant. Your goal is to...";

    public static final String LABEL = "Agent";
    public static final String DESCRIPTION = "Create new agent";
    public static final String BALLERINA = "ballerina";

    private Map<Path, List<TextEdit>> modelProviderTextEdits;

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.AGENT;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return functionKind;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(AGENT_LABEL);
        codedata().node(NodeKind.AGENT).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        modifyAgentProperties(sourceBuilder);

        sourceBuilder
                .token().keyword(SyntaxKind.FINAL_KEYWORD).stepOut()
                .newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode, Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY,
                        Property.SCOPE_KEY, Property.CHECK_ERROR_KEY, NAME, ROLE, INSTRUCTION), true);

        Map<Path, List<TextEdit>> result = sourceBuilder.textEdit().acceptImport().build();

        if (modelProviderTextEdits != null) {
            Map<Path, List<TextEdit>> combinedResult = new LinkedHashMap<>();
            combinedResult.putAll(modelProviderTextEdits);
            combinedResult.putAll(result);
            result = combinedResult;
        }

        return result;
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        if (context == null || context.codedata() == null) {
            throw new IllegalArgumentException("Context or codedata cannot be null");
        }
        if (context.codedata().org().equals(BALLERINA)) {
            functionKind = FunctionData.Kind.CLASS_INIT;
        }
        super.setConcreteTemplateData(context);

        properties().custom()
                .metadata()
                    .label(NAME_LABEL)
                    .description(NAME_DOC)
                    .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .editable()
                .stepOut()
                .addProperty(NAME);

        properties().custom()
                .metadata()
                    .label(ROLE_LABEL)
                    .description(ROLE_DOC)
                    .stepOut()
                .type(Property.ValueType.STRING)
                .placeholder(ROLE_PLACEHOLDER)
                .editable()
                .stepOut()
                .addProperty(ROLE);

        properties().custom()
                .metadata()
                    .label(INSTRUCTION_LABEL)
                    .description(INSTRUCTION_DOC)
                    .stepOut()
                .type(Property.ValueType.STRING)
                .placeholder(INSTRUCTION_PLACEHOLDER)
                .editable()
                .stepOut()
                .addProperty(INSTRUCTION);

        metadata().addData(PARAMS_TO_HIDE, List.of(MODEL, TOOLS, TYPE, MEMORY, SYSTEM_PROMPT, CHECK_ERROR));
    }

    private void modifyAgentProperties(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        if (properties == null) {
            return;
        }

        // Create systemPrompt from role and instruction
        updateSystemPromptProperty(sourceBuilder, properties);

        // Set default values for tools and memory if they're empty
        setDefaultIfEmpty(properties, TOOLS, "[]");
        setDefaultIfEmpty(properties, MEMORY, "()");

        // Create default model parameter if not provided
        Optional<Property> modelProperty = sourceBuilder.getProperty(MODEL);
        if (modelProperty.isEmpty()) {
            createDefaultModelParameter(sourceBuilder, properties);
        }
    }

    private void updateSystemPromptProperty(SourceBuilder sourceBuilder, Map<String, Property> properties) {
        Optional<Property> roleProperty = sourceBuilder.getProperty(ROLE);
        Optional<Property> instructionProperty = sourceBuilder.getProperty(INSTRUCTION);
        if (roleProperty.isPresent() && instructionProperty.isPresent() &&
                roleProperty.get().value() != null && instructionProperty.get().value() != null) {

            String role = roleProperty.get().value().toString();
            String instruction = instructionProperty.get().value().toString();
            String systemPromptValue = String.format("{role: \"%s\", instructions: \"%s\"}", role, instruction);

            Property systemPromptProperty = properties.get(SYSTEM_PROMPT);
            if (systemPromptProperty != null) {
                properties.put(SYSTEM_PROMPT, withNewValue(systemPromptProperty, systemPromptValue));
            }
        }
    }

    private void createDefaultModelParameter(SourceBuilder sourceBuilder, Map<String, Property> properties) {
        Property modelProperty = properties.get(MODEL);
        if (modelProperty != null && (modelProperty.value() == null || modelProperty.value().toString().isEmpty())) {
            String defaultModelValue = findAndCreateDefaultModelProvider(sourceBuilder);
            properties.put(MODEL, withNewValue(modelProperty, defaultModelValue));
        }
    }

    private String findAndCreateDefaultModelProvider(SourceBuilder sourceBuilder) {
        List<Item> modelProviders = LocalIndexCentral.getInstance().getModelProviders();
        AvailableNode defaultModelProviderNode = getDefaultModelProviderNode(modelProviders);

        try {
            NodeBuilder.TemplateContext context = new NodeBuilder.TemplateContext(
                    sourceBuilder.workspaceManager,
                    sourceBuilder.filePath,
                    null,
                    defaultModelProviderNode.codedata(),
                    null
            );

            ModelProviderBuilder modelBuilder = new ModelProviderBuilder();
            modelBuilder.setConcreteTemplateData(context);
            FlowNode modelProviderNode = modelBuilder.build();
            generateModelProviderSourceCode(sourceBuilder, modelProviderNode);

            return modelProviderNode.properties().get(Property.VARIABLE_KEY).value().toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create model provider: " + e.getMessage(), e);
        }
    }

    private static AvailableNode getDefaultModelProviderNode(List<Item> modelProviders) {
        AvailableNode defaultModelProviderNode = null;
        for (Item item : modelProviders) {
            if (item instanceof Category category) {
                for (Item categoryItem : category.items()) {
                    if (categoryItem instanceof AvailableNode modelNode) {
                        if ("OpenAiProvider".equals(modelNode.codedata().object()) ||
                                DEFAULT_MODEL_PROVIDER.equals(modelNode.codedata().symbol())) {
                            defaultModelProviderNode = modelNode;
                            break;
                        }
                    }
                }
                if (defaultModelProviderNode != null) break;
            }
        }

        if (defaultModelProviderNode == null) {
            throw new RuntimeException("Default model provider not found.");
        }
        return defaultModelProviderNode;
    }

    private void generateModelProviderSourceCode(SourceBuilder sourceBuilder, FlowNode modelProviderNode) {
        try {
            ModelProviderBuilder modelBuilder = new ModelProviderBuilder();
            NodeBuilder.TemplateContext context = new NodeBuilder.TemplateContext(
                    sourceBuilder.workspaceManager,
                    sourceBuilder.filePath,
                    null,
                    modelProviderNode.codedata(),
                    null
            );
            modelBuilder.setConcreteTemplateData(context);

            Path projectRoot = sourceBuilder.workspaceManager.projectRoot(sourceBuilder.filePath);
            SourceBuilder modelSourceBuilder =
                    new SourceBuilder(modelProviderNode, sourceBuilder.workspaceManager, projectRoot);

            this.modelProviderTextEdits = modelBuilder.toSource(modelSourceBuilder);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to generate model provider source code during agent creation: " + e.getMessage(), e);
        }
    }

    private void setDefaultIfEmpty(Map<String, Property> properties, String key, String defaultValue) {
        Property property = properties.get(key);
        if (property != null && (property.value() == null || property.value().toString().isEmpty())) {
            properties.put(key, withNewValue(property, defaultValue));
        }
    }

    private Property withNewValue(Property property, Object newValue) {
        return new Property(
                property.metadata(),
                property.valueType(),
                property.valueTypeConstraint(),
                newValue,
                property.oldValue(),
                property.placeholder(),
                property.optional(),
                property.editable(),
                property.advanced(),
                property.hidden(),
                property.modified(),
                property.diagnostics(),
                property.codedata(),
                property.typeMembers(),
                property.advancedValue(),
                property.imports(),
                property.defaultValue(),
                property.comment()
        );
    }
}
