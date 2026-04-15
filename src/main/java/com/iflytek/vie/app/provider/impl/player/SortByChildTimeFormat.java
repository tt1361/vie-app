package com.iflytek.vie.app.provider.impl.player;

import java.util.Comparator;
import java.util.HashMap;

class SortByChildTimeFormat implements Comparator<HashMap> {
   public int compare(HashMap a, HashMap b) {
      return a.get("childTimeFormat").toString().compareTo(b.get("childTimeFormat").toString());
   }
}
