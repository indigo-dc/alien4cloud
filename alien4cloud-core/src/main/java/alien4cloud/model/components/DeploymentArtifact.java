package alien4cloud.model.components;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class DeploymentArtifact implements IArtifact, IPrintable {
    /** This attribute specifies the type of this artifact. */
    @Getter
    private String artifactType;
    /** Specifies the reference of the artifact. */
    @Getter
    private String artifactRef;
    /** Specifies the display name of the artifact. */
    @Getter
    private String artifactName;

    @Getter
    private AbstractPropertyValue file;
    
    /**
     * Non TOSCA compliant property, the artifactRepository indicate where the artifact is stored. It might be in the archive it-self (in this case this
     * property is null), in alien's internal artifact repository (alien) or nexus, git, svn ...
     */
    @Getter
    private String artifactRepository;

    /**
     * The name of the archive in which the original artifact lies.
     */
    @Getter
    private String archiveName;

    /**
     * The version of the archive in which the original artifact lies.
     */
    @Getter
    private String archiveVersion;

    public String getArtifactName() {
        return artifactName != null ? artifactName : artifactRef;
    }
    
    @JsonIgnore
    @Getter
    private boolean printable;
}