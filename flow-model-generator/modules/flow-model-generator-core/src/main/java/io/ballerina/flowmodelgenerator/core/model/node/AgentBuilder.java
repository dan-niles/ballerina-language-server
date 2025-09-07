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
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
        // Modify property values before generating source
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

        return sourceBuilder.textEdit().acceptImport().build();
    }

    private void modifyAgentProperties(SourceBuilder sourceBuilder) {
        Map<String, Property> properties = sourceBuilder.flowNode.properties();
        if (properties == null) {
            return;
        }
        
        // Create systemPrompt from role and instruction
        Optional<Property> roleProperty = sourceBuilder.getProperty(ROLE);
        Optional<Property> instructionProperty = sourceBuilder.getProperty(INSTRUCTION);
        if (roleProperty.isPresent() && instructionProperty.isPresent() && 
            roleProperty.get().value() != null && instructionProperty.get().value() != null) {
            
            String role = roleProperty.get().value().toString();
            String instruction = instructionProperty.get().value().toString();
            String systemPromptValue = String.format("{role: \"%s\", instructions: \"%s\"}", role, instruction);
            
            // Update systemPrompt property
            Property systemPromptProperty = properties.get(SYSTEM_PROMPT);
            if (systemPromptProperty != null) {
                Property newSystemPrompt = new Property(
                    systemPromptProperty.metadata(),
                    systemPromptProperty.valueType(),
                    systemPromptProperty.valueTypeConstraint(),
                    systemPromptValue,
                    systemPromptProperty.oldValue(),
                    systemPromptProperty.placeholder(),
                    systemPromptProperty.optional(),
                    systemPromptProperty.editable(),
                    systemPromptProperty.advanced(),
                    systemPromptProperty.hidden(),
                    systemPromptProperty.modified(),
                    systemPromptProperty.diagnostics(),
                    systemPromptProperty.codedata(),
                    systemPromptProperty.typeMembers(),
                    systemPromptProperty.advancedValue(),
                    systemPromptProperty.imports(),
                    systemPromptProperty.defaultValue(),
                    systemPromptProperty.comment()
                );
                properties.put(SYSTEM_PROMPT, newSystemPrompt);
            }
        }
        
        // Set default values for tools and memory if they're empty
        Property toolsProperty = properties.get(TOOLS);
        if (toolsProperty != null && (toolsProperty.value() == null || toolsProperty.value().toString().isEmpty())) {
            Property newTools = new Property(
                toolsProperty.metadata(), toolsProperty.valueType(), toolsProperty.valueTypeConstraint(),
                "[]", toolsProperty.oldValue(), toolsProperty.placeholder(), toolsProperty.optional(),
                toolsProperty.editable(), toolsProperty.advanced(), toolsProperty.hidden(),
                toolsProperty.modified(), toolsProperty.diagnostics(), toolsProperty.codedata(),
                toolsProperty.typeMembers(), toolsProperty.advancedValue(), toolsProperty.imports(),
                toolsProperty.defaultValue(), toolsProperty.comment()
            );
            properties.put(TOOLS, newTools);
        }
        
        Property memoryProperty = properties.get(MEMORY);
        if (memoryProperty != null && (memoryProperty.value() == null || memoryProperty.value().toString().isEmpty())) {
            Property newMemory = new Property(
                memoryProperty.metadata(), memoryProperty.valueType(), memoryProperty.valueTypeConstraint(),
                "()", memoryProperty.oldValue(), memoryProperty.placeholder(), memoryProperty.optional(),
                memoryProperty.editable(), memoryProperty.advanced(), memoryProperty.hidden(),
                memoryProperty.modified(), memoryProperty.diagnostics(), memoryProperty.codedata(),
                memoryProperty.typeMembers(), memoryProperty.advancedValue(), memoryProperty.imports(),
                memoryProperty.defaultValue(), memoryProperty.comment()
            );
            properties.put(MEMORY, newMemory);
        }
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
}
