package com.iflytek.vie.app.provider.ruleparse.model.tag;

import com.google.common.collect.LinkedHashMultimap;
import java.util.HashMap;

public class TagContainer {
   public String textRule;
   public HashMap<String, Tag> tagMap = new HashMap<>();
   public HashMap<String, String> relativeTagMap = new HashMap<>();
   public LinkedHashMultimap<TagType, Tag> tagTypeMap = LinkedHashMultimap.create();
}
