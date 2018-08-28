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
public class EmailMetaField extends MetaField {

    public final String value;

    @JsonCreator
    public EmailMetaField(@JsonProperty("id") int id,
                          @JsonProperty("name") String name,
                          @JsonProperty("role") Role role,
                          @JsonProperty("isDefault") boolean isDefault,
                          @JsonProperty("icon") String icon,
                          @JsonProperty("value") String value) {
        super(id, name, role, isDefault, icon);
        this.value = value;
    }

    @Override
    public String getNotificationEmail() {
        return value;
    }

    @Override
    public MetaField copySpecificFieldsOnly(MetaField metaField) {
        return new EmailMetaField(id, metaField.name, metaField.role, metaField.isDefault, metaField.icon, value);
    }

    @Override
    public MetaField copy() {
        return new EmailMetaField(id, name, role, isDefault, icon, value);
    }

}
