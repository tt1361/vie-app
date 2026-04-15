package com.iflytek.vie.app.provider.ruleparse.parse;

import com.iflytek.vie.app.provider.ruleparse.model.operator.OperatorType;
import com.iflytek.vie.app.provider.ruleparse.model.operator.StringOperator;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagContainer;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;
import com.iflytek.vie.utils.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TranslateJson {
   public TagContainer tagContainer;

   private class TageRuleHandle extends DefaultHandler {
      private Tag tag;

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
         if ("filter".equals(qName)) {
            this.tag = new Tag();
            String id = attributes.getValue("id");
            String type = attributes.getValue("type");
            this.tag.id = id;
            this.tag.type = TagType.getType(type);
            TranslateJson.this.tagContainer.tagMap.put(id, this.tag);
         }

         if ("rule".equals(qName)) {
            TagProperty property = new TagProperty();
            property.type = TagPropType.getType(attributes.getValue("type"));
            this.tag.tagPropList.add(property);
            OperatorType oprType = OperatorType.getType(attributes.getValue("operator"));
            property.operator = new StringOperator(oprType);
            property.strValue = attributes.getValue("value");
            if (StringUtils.isNullOrEmpry(attributes.getValue("relativeobject"))) {
            }
         }
      }

      @Override
      public void endElement(String uri, String localName, String qName) throws SAXException {
         if ("filter".equals(qName)) {
            this.tag = null;
         }
      }
   }
}
