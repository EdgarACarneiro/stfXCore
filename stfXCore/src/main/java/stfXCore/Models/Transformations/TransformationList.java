package stfXCore.Models.Transformations;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TransformationList {

    private ArrayList<RigidTransformation> transformations;

    TransformationList() {}
}
