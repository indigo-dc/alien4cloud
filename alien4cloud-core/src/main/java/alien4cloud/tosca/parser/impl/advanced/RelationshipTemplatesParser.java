package alien4cloud.tosca.parser.impl.advanced;

import com.google.common.collect.Maps;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RelationshipTemplatesParser
    extends DefaultDeferredParser<Map<String, RelationshipTemplate>> {

  @Resource
  private ScalarParser scalarParser;

  @Resource
  private ICSARRepositorySearchService searchService;

  @Override
  public Map<String, RelationshipTemplate> parse(Node node, ParsingContextExecution context) {
    Object parent = context.getParent();
    if (!(parent instanceof NodeTemplate)) {
      // TODO: throw ex
    }
    NodeTemplate nodeTemplate = (NodeTemplate) parent;
    Map<String, RelationshipTemplate> result = new HashMap<String, RelationshipTemplate>();
    if (!(node instanceof SequenceNode)) {
      // we expect a SequenceNode
      context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_SEQUENCE_EXPECTED, null,
          node.getStartMark(), null, node.getEndMark(), null));
      return null;
    }
    SequenceNode mappingNode = ((SequenceNode) node);
    List<Node> children = mappingNode.getValue();
    for (Node child : children) {
      if (!(child instanceof MappingNode)) {
        context.getParsingErrors()
            .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.YAML_MAPPING_NODE_EXPECTED,
                null, child.getStartMark(), null, child.getEndMark(), null));
        continue;
      }
      MappingNode requirementNode = (MappingNode) child;
      List<NodeTuple> requirementNodeTuples = requirementNode.getValue();
      NodeTuple nt = requirementNodeTuples.get(0);
      Node keyNode = nt.getKeyNode();
      // can a key be something else than a scalar node ?
      String toscaRequirementName = scalarParser.parse(keyNode, context);
      Node valueNode = nt.getValueNode();
      if (valueNode instanceof ScalarNode) {
        // the value node is a scalar, this is the short notation for requirements : Short notation
        // (node only)
        // ex: host: compute
        String toscaRequirementTargetNodeTemplateName = ((ScalarNode) valueNode).getValue();
        buildAndAddRelationhip(valueNode, nodeTemplate, toscaRequirementName,
            toscaRequirementTargetNodeTemplateName, null, null, context, result, null, null);
        continue;
      } else if (valueNode instanceof MappingNode) {
        // the value is not a scalar, Short notation (with relationship or capability) or Extended
        // notation
        // we only parser the Short notation (with relationship or capability)
        MappingNode mappingValueNode = (MappingNode) valueNode;

        // let's search for requirement's properties
        String toscaRequirementTargetNodeTemplateName = null;
        String tosca_capability = null;
        String relationshipType = null;
        Map<String, AbstractPropertyValue> relationshipProperties = null;
        Map<String, Interface> relationshipInterfaces = null;
        for (NodeTuple mappingValueNodeChild : mappingValueNode.getValue()) {
          if (mappingValueNodeChild.getKeyNode() instanceof ScalarNode) {
            String keyNodeName = ((ScalarNode) mappingValueNodeChild.getKeyNode()).getValue();
            if (keyNodeName != null) {
              switch (keyNodeName) {
                case "properties":
                  if (mappingValueNodeChild.getValueNode() instanceof MappingNode) {
                    INodeParser<AbstractPropertyValue> propertyValueParser =
                        context.getRegistry().get("node_template_property");
                    MapParser<AbstractPropertyValue> mapParser =
                        new MapParser<AbstractPropertyValue>(propertyValueParser,
                            "node_template_property");
                    relationshipProperties =
                        mapParser.parse(mappingValueNodeChild.getValueNode(), context);
                  }
                  break;
                case "node":
                  if (mappingValueNodeChild.getValueNode() instanceof ScalarNode) {
                    toscaRequirementTargetNodeTemplateName =
                        ((ScalarNode) mappingValueNodeChild.getValueNode()).getValue();
                  }
                  break;
                case "capability":
                  if (mappingValueNodeChild.getValueNode() instanceof ScalarNode) {
                    tosca_capability =
                        ((ScalarNode) mappingValueNodeChild.getValueNode()).getValue();
                  }
                  break;
                case "relationship":
                  if (mappingValueNodeChild.getValueNode() instanceof ScalarNode) {
                    relationshipType =
                        ((ScalarNode) mappingValueNodeChild.getValueNode()).getValue();
                  } else if (mappingValueNodeChild.getValueNode() instanceof MappingNode) {
                    MappingNode relationshipNode =
                        ((MappingNode) mappingValueNodeChild.getValueNode());
                    for (NodeTuple relationshipNodeChild : relationshipNode.getValue()) {
                      String relationshipNodeChildName =
                          ((ScalarNode) relationshipNodeChild.getKeyNode()).getValue();
                      if (relationshipNodeChildName != null) {
                        switch (relationshipNodeChildName) {
                          case "properties":
                            if (relationshipNodeChild.getValueNode() instanceof MappingNode) {
                              INodeParser<AbstractPropertyValue> propertyValueParser =
                                  context.getRegistry().get("node_template_property");
                              MapParser<AbstractPropertyValue> mapParser =
                                  new MapParser<AbstractPropertyValue>(propertyValueParser,
                                      "node_template_property");
                              relationshipProperties =
                                  mapParser.parse(relationshipNodeChild.getValueNode(), context);
                            }
                            break;
                          case "type":
                            if (relationshipNodeChild.getValueNode() instanceof ScalarNode) {
                              relationshipType =
                                  ((ScalarNode) relationshipNodeChild.getValueNode()).getValue();
                            }
                            break;
                          case "interfaces":
                            if (relationshipNodeChild.getValueNode() instanceof MappingNode) {
                              InterfacesParser interfacesParser =
                                  (InterfacesParser) context.getRegistry().get("interfaces");
                              relationshipInterfaces = interfacesParser
                                  .parse(relationshipNodeChild.getValueNode(), context);
                            }
                            break;
                        }
                      }
                    }
                  }
                  break;
              }
            }

          }
        }

        if (toscaRequirementTargetNodeTemplateName == null) {
          // the node template name is required
          context.getParsingErrors()
              .add(new ParsingError(ErrorCode.REQUIREMENT_TARGET_NODE_TEMPLATE_NAME_REQUIRED, null,
                  valueNode.getStartMark(), null, valueNode.getEndMark(), null));
          continue;
        }
        buildAndAddRelationhip(valueNode, nodeTemplate, toscaRequirementName,
            toscaRequirementTargetNodeTemplateName, tosca_capability, relationshipType, context,
            result, relationshipProperties, relationshipInterfaces);

        // } else {
        // // Relationship type is invalid
        // ParsingError err =
        // new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY,
        // "Parsing a MappingNode as a Map", mappingValueNode.getStartMark(),
        // "The value of this tuple should be either a string or a Relationship",
        // mappingValueNode.getEndMark(), "relationship");
        // context.getParsingErrors().add(err);
        // }

      }

    }
    return result;

  }

  private Node getChildNodeByName(MappingNode mappingValueNode, ParsingContextExecution context,
      String nodeName) {
    for (NodeTuple mappingValueNodeChilds : mappingValueNode.getValue()) {
      if (mappingValueNodeChilds.getKeyNode() instanceof ScalarNode
          && ((ScalarNode) mappingValueNodeChilds.getKeyNode()).getValue().equals(nodeName))
        return mappingValueNodeChilds.getValueNode();
    }
    return null;
  }

  private Map<String, AbstractPropertyValue> getRelationshipProperties(MappingNode mappingValueNode,
      ParsingContextExecution context) {
    Map<String, AbstractPropertyValue> relationshipProperties = null;
    for (NodeTuple mappingValueNodeChilds : mappingValueNode.getValue()) {
      if (mappingValueNodeChilds.getKeyNode() instanceof ScalarNode
          && ((ScalarNode) mappingValueNodeChilds.getKeyNode()).getValue().equals("properties")
          && (mappingValueNodeChilds.getValueNode() instanceof MappingNode)) {
        INodeParser<AbstractPropertyValue> propertyValueParser =
            context.getRegistry().get("node_template_property");
        MapParser<AbstractPropertyValue> mapParser =
            new MapParser<AbstractPropertyValue>(propertyValueParser, "node_template_property");
        relationshipProperties = mapParser.parse(mappingValueNodeChilds.getValueNode(), context);
      }
    }

    return relationshipProperties;
  }

  private void buildAndAddRelationhip(Node node, NodeTemplate nodeTemplate,
      String toscaRequirementName, String toscaRequirementTargetNodeTemplateName,
      String capabilityType, String relationshipType, ParsingContextExecution context,
      Map<String, RelationshipTemplate> relationships,
      Map<String, AbstractPropertyValue> relationshipProperties,
      Map<String, Interface> relationshipInterfaces) {
    RelationshipTemplate relationshipTemplate = buildRelationshipTemplate(node, nodeTemplate,
        toscaRequirementName, toscaRequirementTargetNodeTemplateName, capabilityType,
        relationshipType, context, relationshipProperties, relationshipInterfaces);
    if (relationshipTemplate == null) {
      context.getParsingErrors()
          .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.RELATIONSHIP_NOT_BUILT, null,
              node.getStartMark(), null, node.getEndMark(), toscaRequirementName));
    } else {
      String relationShipName = buildRelationShipTemplateName(relationshipTemplate,
          toscaRequirementTargetNodeTemplateName);
      addRelationshipTemplateToMap(relationships, relationShipName, relationshipTemplate, 0);
    }
  }

  private void addRelationshipTemplateToMap(Map<String, RelationshipTemplate> map, String name,
      RelationshipTemplate relationshipTemplate, int attempCount) {
    String key = name;
    if (attempCount > 0) {
      key += attempCount;
    }
    if (map.containsKey(key)) {
      addRelationshipTemplateToMap(map, name, relationshipTemplate, attempCount++);
    } else {
      map.put(key, relationshipTemplate);
    }
  }

  private String buildRelationShipTemplateName(RelationshipTemplate relationshipTemplate,
      String targetName) {
    String value = relationshipTemplate.getType();
    if (value.contains(".")) {
      value = value.substring(value.lastIndexOf(".") + 1);
    }
    value = StringUtils.uncapitalize(value);
    value = value + StringUtils.capitalize(targetName);
    return value;
  }

  private RelationshipTemplate buildRelationshipTemplate(Node node, NodeTemplate nodeTemplate,
      String toscaRequirementName, String toscaRequirementTargetNodeTemplateName,
      String capabilityType, String relationshipType, ParsingContextExecution context,
      Map<String, AbstractPropertyValue> relationshipProperties,
      Map<String, Interface> relationshipInterfaces) {
    RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
    ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
    IndexedNodeType indexedNodeType = ToscaParsingUtil
        .getNodeTypeFromArchiveOrDependencies(nodeTemplate.getType(), archiveRoot, searchService);
    if (indexedNodeType == null) {
      // the node type is null if not found in archive or dep, the error is already raised
      return null;
    }
    RequirementDefinition rd = getRequirementDefinitionByNameInHierarchy(indexedNodeType,
        toscaRequirementName, archiveRoot);
    if (rd == null) {
      context.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_NOT_FOUND, null,
          node.getStartMark(), null, node.getEndMark(), toscaRequirementName));
      return null;
    }
    // the relationship template type is taken from 'relationship' node or requirement definition
    String relationshipTypeName =
        (relationshipType != null) ? relationshipType : rd.getRelationshipType();
    // ex: host
    relationshipTemplate.setRequirementName(toscaRequirementName);
    // ex: tosca.nodes.Compute
    relationshipTemplate.setRequirementType(rd.getType());
    // ex: tosca.relationships.HostedOn
    relationshipTemplate.setType(relationshipTypeName);

    // now find the target of the relation
    NodeTemplate targetNodeTemplate =
        archiveRoot.getTopology().getNodeTemplates().get(toscaRequirementTargetNodeTemplateName);
    if (targetNodeTemplate == null) {
      context.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_TARGET_NOT_FOUND, null,
          node.getStartMark(), null, node.getEndMark(), toscaRequirementTargetNodeTemplateName));
      return null;
    }
    // IndexedNodeType indexedTargetNodeType =
    // ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(targetNodeTemplate.getType(),
    // archiveRoot,
    // searchService);
    // if (!indexedTargetNodeType.getDerivedFrom().contains(rd.getType())) {
    // an error ?
    // context.getParsingErrors().add(
    // new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.VALIDATION_ERROR, "node_template
    // requirements parsing", node.getStartMark(),
    // "The relation target doesn't seem to be compibatble with the requirement", node.getEndMark(),
    // targetNodeTemplate.getType()));
    // }

    Capability capability = null;
    if (capabilityType == null) {
      // the capability type is not known, we assume that we are parsing a Short notation (node
      // only)
      // in such notation : "a requirement named ‘host’ that needs to be fulfilled by the same named
      // capability"
      // so here we use the requirement name to find the capability
      if (targetNodeTemplate.getCapabilities() != null) {
        capability = targetNodeTemplate.getCapabilities().get(toscaRequirementName);
        if (capability != null) {
          relationshipTemplate.setTargetedCapabilityName(rd.getId());
        }
      }
    } else {
      Entry<String, Capability> capabilityEntry =
          getCapabilityByType(targetNodeTemplate, capabilityType);
      if (capabilityEntry != null) {
        capability = capabilityEntry.getValue();
        relationshipTemplate.setTargetedCapabilityName(capabilityEntry.getKey());
      }
    }
    if (capability == null) {
      // we should fail
      context.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_CAPABILITY_NOT_FOUND,
          null, node.getStartMark(), null, node.getEndMark(), toscaRequirementName));
      return null;
    }

    relationshipTemplate.setTarget(toscaRequirementTargetNodeTemplateName);

    // now find the relationship type
    IndexedRelationshipType indexedRelationshipType =
        ToscaParsingUtil.getRelationshipTypeFromArchiveOrDependencies(relationshipTypeName,
            archiveRoot, searchService);
    if (indexedRelationshipType == null) {
      context.getParsingErrors().add(new ParsingError(ErrorCode.TYPE_NOT_FOUND, null,
          node.getStartMark(), null, node.getEndMark(), relationshipTypeName));
      return null;
    }
    Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
    TopologyServiceCore.fillProperties(properties, indexedRelationshipType.getProperties(),
        relationshipProperties);
    relationshipTemplate.setProperties(properties);
    relationshipTemplate.setAttributes(indexedRelationshipType.getAttributes());

    Map<String, Interface> interfaces = IndexedModelUtils
        .mergeInterfaces(indexedRelationshipType.getInterfaces(), relationshipInterfaces, true);
    relationshipTemplate.setInterfaces(interfaces);
    return relationshipTemplate;
  }

  private Entry<String, Capability> getCapabilityByType(NodeTemplate nodeTemplate, String type) {
    for (Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
      if (capabilityEntry.getValue().getType().equals(type)) {
        return capabilityEntry;
      }
    }
    return null;
  }

  private RequirementDefinition getRequirementDefinitionByNameInHierarchy(
      IndexedNodeType indexedNodeType, String name, ArchiveRoot archiveRoot) {
    RequirementDefinition rd = getRequirementDefinitionByName(indexedNodeType, name);
    if (rd != null) {
      return rd;
    }
    List<String> derivedFrom = indexedNodeType.getDerivedFrom();
    if (derivedFrom == null) {
      return null;
    }
    Map<String, IndexedNodeType> hierarchy = Maps.newHashMap();
    for (String parentId : derivedFrom) {
      IndexedNodeType parentType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(parentId,
          archiveRoot, searchService);
      hierarchy.put(parentType.getId(), parentType);
    }
    List<IndexedNodeType> hierarchyList = IndexedModelUtils.orderByDerivedFromHierarchy(hierarchy);
    Collections.reverse(hierarchyList);
    for (IndexedNodeType parentType : hierarchyList) {
      rd = getRequirementDefinitionByName(parentType, name);
      if (rd != null) {
        return rd;
      }
    }
    return null;
  }

  private RequirementDefinition getRequirementDefinitionByName(IndexedNodeType indexedNodeType,
      String name) {
    if (indexedNodeType.getRequirements() != null) {
      for (RequirementDefinition rd : indexedNodeType.getRequirements()) {
        if (rd.getId().equals(name)) {
          return rd;
        }
      }
    }
    return null;
  }

}