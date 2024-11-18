package digit.web.models;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import digit.web.models.Boundary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;

/**
 * Boundary
 */
@Validated
@jakarta.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2024-11-18T11:00:08.741990949+05:30[Asia/Kolkata]")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Boundary {
        @JsonProperty("code")
        @NotNull

        private String code = null;

        @JsonProperty("name")
        @NotNull

        private String name = null;

        @JsonProperty("label")

        private String label = null;

        @JsonProperty("latitude")

        private Double latitude = null;

        @JsonProperty("longitude")

        private Double longitude = null;

        @JsonProperty("children")
        @Valid
        private List<Boundary> children = null;

        @JsonProperty("materializedPath")

        private String materializedPath = null;

        public Boundary addChildrenItem(Boundary childrenItem) {
                if (this.children == null) {
                        this.children = new ArrayList<>();
                }
                this.children.add(childrenItem);
                return this;
        }

}
