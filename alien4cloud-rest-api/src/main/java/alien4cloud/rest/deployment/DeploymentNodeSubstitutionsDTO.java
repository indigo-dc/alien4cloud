package alien4cloud.rest.deployment;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.LocationResourceTypes;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@ApiModel("Contains the types and templates of resources that can be substituted for a deployment.")
public class DeploymentNodeSubstitutionsDTO {

    @ApiModelProperty(value = "Map of node id to list of available location resource templates' id.")
    private Map<String, Set<String>> availableSubstitutions;

    @ApiModelProperty(value = "Map of location resource id to location resource template.")
    private Map<String, LocationResourceTemplate> substitutionsTemplates;

    @ApiModelProperty(value = "Location resources types contain types for the templates.")
    private LocationResourceTypes substitutionTypes;
}
