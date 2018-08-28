package cc.blynk.server.core.model.web.product.metafields;

import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.product.MetaField;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.17.
 */
public class TimeMetaField extends MetaField {

    public final long time;

    @JsonCreator
    public TimeMetaField(@JsonProperty("id") int id,
                         @JsonProperty("name") String name,
                         @JsonProperty("role") Role role,
                         @JsonProperty("isDefault") boolean isDefault,
                         @JsonProperty("icon") String icon,
                         @JsonProperty("time") long time) {
        super(id, name, role, isDefault, icon);
        this.time = time;
    }

    @Override
    public MetaField copySpecificFieldsOnly(MetaField metaField) {
        return new TimeMetaField(id, metaField.name, metaField.role, metaField.isDefault, metaField.icon, time);
    }

    @Override
    public MetaField copy() {
        return new TimeMetaField(id, name, role, isDefault, icon, time);
    }
}
