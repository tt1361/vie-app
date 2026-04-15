package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.config.ModelCategory;
import com.iflytek.vie.app.pojo.config.ModelCategoryRelation;
import com.iflytek.vie.app.pojo.config.ModelCategoryRequest;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelCategoryDB {
   private static final Logger logger = LoggerFactory.getLogger(ModelCategoryDB.class);
   private HibernateTemplateUtils hiberTemp;

   public Long saveModelCategory(ModelCategory modelCategory) {
      try {
         this.hiberTemp.save(modelCategory);
         return modelCategory.getId();
      } catch (Exception var3) {
         logger.error("saveModelCategory DB has a error!", var3);
         return null;
      }
   }

   public boolean checkModelCategoryNameExist(ModelCategoryRequest modelCategoryRequest) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "FROM ModelCategory WHERE modelCategoryName = :modelCategoryName ";
         if (modelCategoryRequest.getId() != null) {
            hql = hql + "AND id != :id ";
            paramNames.add("id");
            values.add(modelCategoryRequest.getId());
         }

         paramNames.add("modelCategoryName");
         values.add(modelCategoryRequest.getModelCategoryName());
         List<ModelCategory> list = (List<ModelCategory>)(List<?>)this.hiberTemp.createQuery(hql, paramNames, values);
         if (list != null && list.size() > 0) {
            return true;
         }
      } catch (Exception var6) {
         logger.error("checkModelCategoryNameExist DB has a error!", var6);
      }

      return false;
   }

   public Long saveModelCategoryRelation(ModelCategoryRelation modelCategoryRelation) {
      try {
         this.hiberTemp.save(modelCategoryRelation);
         return modelCategoryRelation.getModelCatagoryId();
      } catch (Exception var3) {
         logger.error("saveModelCategoryRelation DB has a error!", var3);
         return null;
      }
   }

   public void deleteModelCategory(List<Long> ids) {
      try {
         this.hiberTemp.bulkUpdate("DELETE FROM ModelCategory WHERE id IN (:ids)", new String[]{"ids"}, new Object[]{ids});
      } catch (Exception var3) {
         logger.error("deleteModelCategory DB has a error!", var3);
      }
   }

   public void deleteModelCategoryRelation(List<Long> ids) {
      try {
         this.hiberTemp.bulkUpdate("DELETE FROM ModelCategoryRelation WHERE modelCatagoryId IN (:ids)", new String[]{"ids"}, new Object[]{ids});
      } catch (Exception var3) {
         logger.error("deleteModelCategoryRelation DB has a error!", var3);
      }
   }

   public void updateModelCategoryStatus(List<Long> ids, Integer status) {
      try {
         this.hiberTemp.bulkUpdate("UPDATE ModelCategory SET status = :status  WHERE id IN (:ids)", new String[]{"status", "ids"}, new Object[]{status, ids});
      } catch (Exception var4) {
         logger.error("updateModelCategoryStatus DB has a error!", var4);
      }
   }

   public void deleteModelCategoryRelation(Long id) {
      try {
         this.hiberTemp.bulkUpdate("DELETE FROM ModelCategoryRelation WHERE modelCatagoryId = :id", new String[]{"id"}, new Object[]{id});
      } catch (Exception var3) {
         logger.error("deleteModelCategoryRelation DB has a error!", var3);
      }
   }

   public ModelCategory getModelCategoryById(Long id) {
      try {
         return (ModelCategory)this.hiberTemp.get(ModelCategory.class, id);
      } catch (Exception var3) {
         logger.error("getModelCategoryById DB has a error!", var3);
         return null;
      }
   }

   public List<Map<String, String>> queryRelateModelList(Long id) {
      List<Map<String, String>> result = new ArrayList<>();

      try {
         String sql = "SELECT mc.MODELID, mi.model_name FROM model_category_releassion mc LEFT JOIN MODEL_INFO mi ON mc.MODELID = mi.model_id WHERE mc.MODELCATAGORYID = :id ";
         List<Object[]> list = (List<Object[]>)(List<?>)this.hiberTemp.createSQLQuery(sql, new String[]{"id"}, new Object[]{id});
         if (list != null && list.size() > 0) {
            Object[] model = null;

            for (int i = 0; i < list.size(); i++) {
               model = list.get(i);
               if (model != null && model.length > 0) {
                  Map<String, String> map = new HashMap<>();
                  map.put("modelId", String.valueOf(model[0]));
                  map.put("modelName", String.valueOf(model[1]));
                  result.add(map);
               }
            }
         }
      } catch (Exception var8) {
         logger.error("queryRelateModelList DB has a error!", var8);
      }

      return result;
   }

   public int queryModelCategoryTotalCount(ModelCategoryRequest modelCategoryRequest, List<Long> idList) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      StringBuffer sb = new StringBuffer();
      int count = 0;

      try {
         sb.append("SELECT COUNT(1) FROM model_category mc WHERE mc.modelcategorytype = :modelcategorytype ");
         if (!StringUtils.isNullOrEmpry(modelCategoryRequest.getModelCategoryName())) {
            sb.append("AND mc.modelcategoryname like :modelcategoryname ");
            paramNames.add("modelcategoryname");
            values.add("%" + modelCategoryRequest.getModelCategoryName() + "%");
         }

         sb.append("AND mc.id in (:id) ");
         paramNames.add("modelcategorytype");
         values.add(modelCategoryRequest.getModelCategoryType());
         paramNames.add("id");
         values.add(idList);
         Object obj = this.hiberTemp.uniqueSQLResult(sb.toString(), paramNames, values);
         if (obj != null) {
            count = Integer.parseInt(String.valueOf(obj));
         }

         return count;
      } catch (Exception var8) {
         logger.error("queryModelCategoryTotalCount DB has a error!", var8);
         return 0;
      }
   }

   public Map<Long, String> queryModelMap(List<Long> categoryIds) {
      Map<Long, String> map = new HashMap<>();
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      StringBuffer sb = new StringBuffer();

      try {
         sb.append(
            "SELECT mr.modelcatagoryid, mi.model_name FROM model_category_releassion mr LEFT JOIN MODEL_INFO mi ON mr.modelid = mi.model_id WHERE mr.modelcatagoryid IN (:modelcatagoryid) "
         );
         paramNames.add("modelcatagoryid");
         values.add(categoryIds);
         List<Object[]> list = (List<Object[]>)(List<?>)this.hiberTemp.createSQLQuery(sb.toString(), paramNames, values);
         if (list != null && list.size() > 0) {
            map = new HashMap<>();
            Object[] obj = null;
            String value = "";
            Long key = null;

            for (int i = 0; i < list.size(); i++) {
               obj = list.get(i);
               if (obj[1] != null) {
                  key = Long.parseLong(String.valueOf(obj[0]));
                  if (map.containsKey(key)) {
                     value = map.get(key);
                     map.put(key, value + "," + obj[1]);
                  } else {
                     map.put(key, String.valueOf(obj[1]));
                  }
               }
            }
         }

         return map;
      } catch (Exception var11) {
         logger.error("queryModelMap method has a error!", var11);
         return null;
      }
   }

   public List<ModelCategory> queryModelCategoryBeanList(ModelCategoryRequest modelCategoryRequest, List<Long> idList) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      StringBuffer sb = new StringBuffer();

      try {
         sb.append("FROM ModelCategory mc WHERE mc.modelCategoryType = :modelcategorytype ");
         if (!StringUtils.isNullOrEmpry(modelCategoryRequest.getModelCategoryName())) {
            sb.append("AND mc.modelCategoryName like :modelcategoryname ");
            paramNames.add("modelcategoryname");
            values.add("%" + modelCategoryRequest.getModelCategoryName() + "%");
         }

         sb.append("AND mc.id in (:id) ");
         sb.append("ORDER BY mc.createdate desc");
         paramNames.add("modelcategorytype");
         values.add(modelCategoryRequest.getModelCategoryType());
         paramNames.add("id");
         values.add(idList);
         return (List)this.hiberTemp.createQuery(modelCategoryRequest.getPageNum(), modelCategoryRequest.getPageSize(), sb.toString(), paramNames, values);
      } catch (Exception var7) {
         logger.error("queryModelCategoryBeanList DB has a error!", var7);
         return null;
      }
   }

   public ModelCategory queryModelCategoryByName(String modelCategoryName) {
      List<String> params = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "FROM ModelCategory mc WHERE mc.modelCategoryName = :modelCategoryName ";
         params.add("modelCategoryName");
         values.add(modelCategoryName);
         return (ModelCategory)this.hiberTemp.uniqueResult(hql, params, values);
      } catch (Exception var5) {
         logger.error("queryModelCategoryByName DB has a error!", var5);
         return null;
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
