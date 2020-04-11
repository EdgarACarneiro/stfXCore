package stfXCore.Models.Storyboard.Transformations;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;

@Data
public class RigidTransformation implements Serializable {

    private float rotation;

    private ArrayList<Float> translation;

    /**
     * Rigid registration only considers uniform scaling
     */
    private float scale;

    @Builder
    public RigidTransformation() {}
}